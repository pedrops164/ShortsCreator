
import { AuthOptions, Account, Session } from "next-auth";
import { JWT } from "next-auth/jwt";
import KeycloakProvider from "next-auth/providers/keycloak";
import GoogleProvider from "next-auth/providers/google";

// This variable will hold the promise of the ongoing token refresh.
let refreshTokenPromise: Promise<JWT> | null = null;

interface RefreshedTokens {
  access_token: string;
  refresh_token?: string; // Keycloak may or may not return a new refresh token
  expires_in: number;
}

// This function will handle the token refresh logic
async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const url = `${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`;
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        client_id: String(process.env.KEYCLOAK_CLIENT_ID),
        client_secret: String(process.env.KEYCLOAK_CLIENT_SECRET),
        grant_type: 'refresh_token',
        refresh_token: String(token.refreshToken),
      }),
    });

    const refreshedTokens: RefreshedTokens = await response.json();

    if (!response.ok) {
      throw refreshedTokens;
    }

    // Return the new token information
    return {
      ...token,
      accessToken: refreshedTokens.access_token,
      accessTokenExpires: Date.now() + refreshedTokens.expires_in * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken, // Fall back to old refresh token
    };
  } catch (error) {
    console.error('Error refreshing access token', error);
    // Indicate that the refresh failed
    return {
      ...token,
      error: 'RefreshAccessTokenError',
    };
  }
}

export const authOptions: AuthOptions = {
  providers: [
    /* KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,
      issuer: process.env.KEYCLOAK_ISSUER!,
    }), */
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID!,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
    }),
  ],
  callbacks: {
    async jwt({ token, account }: { token: JWT; account: Account | null; }): Promise<JWT> {
      // On initial sign-in, persist the token details
      if (account) {
        token.accessToken = account.access_token!;
        token.refreshToken = account.refresh_token!;
        token.accessTokenExpires = account.expires_at * 1000;
        return token;
      }

      // If the access token has not expired, return it
      console.log('Checking access token expiration...');
      console.log(`Token expires at: ${new Date(token.accessTokenExpires).toISOString()}`);
      if (Date.now() < token.accessTokenExpires) {
        return token;
      }

      // Lock to prevent race conditions
      if (!refreshTokenPromise) {
        console.log('Access token expired, refreshing...');
        refreshTokenPromise = refreshAccessToken(token);
      }

      const refreshedToken = await refreshTokenPromise;
      
      // Clear the promise after completion
      refreshTokenPromise = null;

      return refreshedToken;
    },
    async session({ session, token }: { session: Session; token: JWT; }): Promise<Session> {
      // Send properties to the client, like an access_token from a provider.
      session.accessToken = token.accessToken;
      session.error = token.error; // Pass potential error to the client
      return session;
    },
  },
  pages: {
    signIn: '/login',
    // Can also add custom pages for signOut, error, etc.
  },
  secret: process.env.NEXTAUTH_SECRET,
};
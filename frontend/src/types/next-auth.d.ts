import NextAuth, { DefaultSession } from "next-auth"
import { JWT } from "next-auth/jwt"

declare module "next-auth" {
  /**
   * Returned by `useSession`, `getSession` and received as a prop on the `SessionProvider` React Context
   */
  interface Session {
    accessToken?: string;
    user: {
      // You can add other properties here if needed
    } & DefaultSession["user"]
    error?: string; // Pass errors to the client
  }
  
  /** The OAuth account returned by the provider */
  interface Account {
    expires_at: number;
    refresh_token: string;
  }
}

declare module "next-auth/jwt" {
  /** Returned by the `jwt` callback and `getToken`, when using JWT sessions */
  interface JWT {
    /** OpenID ID Token */
    accessToken?: string
    refreshToken: string;
    accessTokenExpires: number;
    error?: string; // To handle refresh errors
  }
}
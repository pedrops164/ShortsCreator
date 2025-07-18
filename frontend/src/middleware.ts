export { default } from "next-auth/middleware"

export const config = { 
  matcher: [
    "/content",
    "/drafts",
    "/editor/:path*", // protect all editor routes
    "/account",
  ] 
};
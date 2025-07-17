export { default } from "next-auth/middleware"

export const config = { 
  matcher: [
    "/dashboard", 
    "/content",
    "/drafts",
    "/editor/:path*", // protect all editor routes
    "/account",
  ] 
};
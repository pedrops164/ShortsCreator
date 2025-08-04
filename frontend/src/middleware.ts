export { default } from "next-auth/middleware"

export const config = { 
  matcher: [
    "/content",
    "/editor/:path*", // protect all editor routes
    "/account",
  ] 
};
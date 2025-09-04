import type { NextConfig } from "next";

const isProduction = process.env.NODE_ENV === 'production';

const nextConfig: NextConfig = {
  output: 'standalone',
  // Allow linting and type checking during build for better code quality
  eslint: {
    ignoreDuringBuilds: false,
  },
  typescript: {
    ignoreBuildErrors: false,
  },
  // allow images from the asset CDN
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'd3hucckltpi95a.cloudfront.net',
      },
    ],
    // Only use the custom loader in production
    loader: isProduction ? 'custom' : 'default',
    loaderFile: isProduction ? './src/lib/image-loader.js' : undefined,
  },
};

export default nextConfig;

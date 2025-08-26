import { NextRequest } from 'next/server';
import { getToken } from 'next-auth/jwt';

const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081/api/v1';

// A handler function to avoid repeating code
async function proxyHandler(req: NextRequest) {
  const token = await getToken({ req });

  if (!token?.accessToken) {
    return new Response('Unauthorized', { status: 401 });
  }

  // Reconstruct the destination URL from the [...path] parameter
  const path = req.nextUrl.pathname.replace('/api/v1/', '');
  const destination = `${backendUrl}/${path}${req.nextUrl.search}`;
  
  // Prepare the request body for non-GET requests
  const body = req.method !== 'GET' ? await req.text() : undefined;

  try {
    const response = await fetch(destination, {
      method: req.method,
      headers: {
        'Authorization': `Bearer ${token.accessToken}`,
        'Content-Type': 'application/json',
      },
      body,
    });
    
    // Return the response from the backend directly to the client
    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
    });

  } catch (error) {
    console.error('API Proxy Error:', error);
    return new Response('Internal Server Error', { status: 500 });
  }
}

export async function GET(request: NextRequest) {
  return proxyHandler(request);
}

export async function POST(request: NextRequest) {
  return proxyHandler(request);
}

export async function PUT(request: NextRequest) {
  return proxyHandler(request);
}

export async function DELETE(request: NextRequest) {
  return proxyHandler(request);
}
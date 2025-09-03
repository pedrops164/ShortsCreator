import { NextRequest } from 'next/server';
import { getToken } from 'next-auth/jwt';

const sseUrl = process.env.NEXT_PUBLIC_API_URL + '/notifications'; // Java SSE endpoint

export async function GET(req: NextRequest) {
  const token = await getToken({ req });

  if (!token?.accessToken) {
    return new Response('Unauthorized', { status: 401 });
  }

  try {
    const response = await fetch(sseUrl, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token.accessToken}`,
        'Accept': 'text/event-stream',
      },
    });

    if (!response.ok || !response.body) {
      throw new Error(`Backend SSE error: ${response.status}`);
    }

    // Create a ReadableStream to pipe the data from the backend to the client
    const stream = new ReadableStream({
      start(controller) {
        const reader = response.body!.getReader();
        function push() {
          reader.read().then(({ done, value }) => {
            if (done) {
              controller.close();
              return;
            }
            controller.enqueue(value);
            push();
          }).catch(error => {
            console.error('Error reading from backend stream:', error);
            controller.error(error);
          });
        }
        push();
      }
    });

    return new Response(stream, {
      status: 200,
      headers: {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
      },
    });

  } catch (error) {
    console.error("SSE Proxy Error:", error);
    return new Response('Internal Server Error', { status: 500 });
  }
}
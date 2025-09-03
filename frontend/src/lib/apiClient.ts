import axios, { isAxiosError } from 'axios';

// Define a custom error class
export class ApiError extends Error {
  public readonly status: number;
  public readonly errorCode: string;

  constructor(message: string, status: number, errorCode: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errorCode = errorCode;
  }

  public static createDefault(): ApiError {
    return new ApiError(
      'An error occurred while processing your request.',
      500,
      'UNKNOWN_ERROR'
    );
  }
}

const apiClient = axios.create({
  // The baseURL points to the Next.js server's API routes.
  baseURL: process.env.NEXT_PUBLIC_API_URL, 
  headers: {
    'Content-Type': 'application/json',
  },
});

// --- Add an error interceptor ---
apiClient.interceptors.response.use(
  (response) => response, // Pass through successful responses
  (error) => {
    // Check if it's an Axios error and has the response data we expect
    if (isAxiosError(error) && error.response?.data?.errorCode) {
      const { status, data } = error.response;
      const { message, errorCode } = data;
      // Re-throw our custom, structured error
      return Promise.reject(new ApiError(message, status, errorCode));
    }
    // For all other errors, just let Axios handle it
    return Promise.reject(error);
  }
);

export default apiClient;
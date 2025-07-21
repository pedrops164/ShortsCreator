
import axios from 'axios';

const apiClient = axios.create({
  // The baseURL now points to our Next.js server's API routes.
  baseURL: '/api/v1', 
  headers: {
    'Content-Type': 'application/json',
  },
});

export default apiClient;
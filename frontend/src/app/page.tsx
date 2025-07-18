import { redirect } from 'next/navigation';

export default function HomePage() {
  // This will redirect users from '/' to '/create'
  redirect('/create');
}
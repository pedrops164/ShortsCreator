/**
 * This is the component for the homepage (the "/" route).
 * The surrounding layout with the navbar is automatically applied by `app/layout.tsx`.
 */
export default function HomePage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-yellow-500">Welcome!</h1>
      <p className="mt-4 text-gray-300">This is your main dashboard. Select an option from the navigation on the left to get started.</p>
    </div>
  );
}

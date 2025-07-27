// --- Reusable Sub-Components (can be in their own files) ---
import React from 'react';

// Shared components for form fields, selects, and toggle switches
const FormSection = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <div className="space-y-4 p-4 border border-accent/50 rounded-lg bg-background/20">
    <h3 className="font-semibold text-lg text-foreground">{title}</h3>
    <div className="space-y-4">{children}</div>
  </div>
);

const FormField = ({ label, name, as = 'input', error, ...props }) => (
  <div className="w-full">
    <label htmlFor={name} className="block text-sm font-medium text-foreground/80 mb-1">{label}</label>
    {as === 'textarea'
      ? <textarea id={name} name={name} {...props} className={`block w-full bg-background/50 border rounded-md p-2 focus:ring-primary/50 ${error ? 'border-red-500' : 'border-accent'}`} />
      : <input id={name} name={name} {...props} className={`block w-full bg-background/50 border rounded-md p-2 focus:ring-primary/50 ${props.type === 'color' ? 'p-1' : ''} ${error ? 'border-red-500' : 'border-accent'}`} />
    }
    {error && <p className="text-red-500 text-xs mt-1">{error}</p>}
  </div>
);

const FormSelect = ({ label, name, children, error, ...props }) => (
  <div>
    <label htmlFor={name} className="block text-sm font-medium text-foreground/80 mb-1">{label}</label>
    <select id={name} name={name} {...props} className={`block w-full bg-background/50 border rounded-md p-2 focus:ring-primary/50 ${error ? 'border-red-500' : 'border-accent'}`}>
      {children}
    </select>
    {error && <p className="text-red-500 text-xs mt-1">{error}</p>}
  </div>
);

const ToggleSwitch = ({ name, enabled, onChange }) => (
  <label htmlFor={name} className="inline-flex items-center cursor-pointer">
    <input id={name} name={name} type="checkbox" checked={enabled} onChange={onChange} className="sr-only peer" />
    <div className="relative w-11 h-6 bg-accent/50 rounded-full peer peer-focus:ring-2 peer-focus:ring-primary/50 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
  </label>
);

export { FormSection, FormField, FormSelect, ToggleSwitch };
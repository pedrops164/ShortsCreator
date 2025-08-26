import React from 'react';
import { Label } from './label';
import { LucideIcon } from 'lucide-react';

interface FormControlProps {
  label: string;
  children: React.ReactNode;
  error?: string;
  icon?: LucideIcon; // Optional icon component
  required?: boolean;
}

export const FormControl = ({ label, children, error, icon: Icon, required }: FormControlProps) => {
  const labelText = `${label}${required ? ' *' : ''}`;

  return (
    <div className={error ? 'rounded-md border border-destructive p-3' : ''}>
      <Label className="font-semibold flex items-center mb-2">
        {Icon && <Icon className="w-4 h-4 mr-2" />}
        {labelText}
      </Label>
      {children}
      {error && <p className="text-sm text-destructive mt-2">{error}</p>}
    </div>
  );
};
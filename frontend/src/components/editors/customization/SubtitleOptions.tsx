import { FormField, FormSelect, ToggleSwitch } from '@/components/editors/customization/common'; // Assuming shared components are moved

// Define props for the component
interface SubtitleOptionsProps {
  params: {
    show: boolean;
    font: string;
    position: string;
    color: string;
  };
  errors?: any;
  onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
}

export function SubtitleOptions({ params, errors, onChange }: SubtitleOptionsProps) {
  return (
    <details className="p-4 border rounded-lg border-accent/50" open>
      <summary className="font-semibold cursor-pointer text-foreground">Subtitle Options</summary>
      <div className="grid grid-cols-1 gap-4 mt-4 md:grid-cols-2 lg:grid-cols-3">
        <div className="flex items-center pt-6 space-x-4">
          <label htmlFor="show" className="text-sm font-medium text-foreground/80">Show Subtitles</label>
          <ToggleSwitch name="show" enabled={params.show} onChange={onChange} />
        </div>
        <FormField label="Font" name="font" value={params.font} onChange={onChange} disabled={!params.show} error={errors?.font} />
        <FormSelect label="Position" name="position" value={params.position} onChange={onChange} disabled={!params.show}>
          <option value="bottom">Bottom</option>
          <option value="center">Center</option>
          <option value="top">Top</option>
        </FormSelect>
        <FormField label="Color" name="color" type="color" value={params.color} onChange={onChange} className="w-16 h-10" disabled={!params.show} error={errors?.color} />
      </div>
    </details>
  );
}
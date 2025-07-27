import { FormSection, FormSelect } from '@/components/editors/customization/common';

// The main wrapper component provides the section title and grid layout
function VideoOptions({ children }: { children: React.ReactNode }) {
  return (
    <FormSection title="Video Customization">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        {children}
      </div>
    </FormSection>
  );
}

// --- Sub-Components ---

const BackgroundVideo = ({ value, onChange, error }) => (
  <FormSelect label="Background Video" name="backgroundVideoId" value={value} onChange={onChange} error={error}>
    <option value="minecraft1">Minecraft Parkour</option>
    <option value="gta1">GTA Gameplay</option>
  </FormSelect>
);

const Music = ({ value, onChange, error }) => (
  <FormSelect label="Background Music" name="backgroundMusicId" value={value} onChange={onChange} error={error}>
    <option value="">None</option>
    <option value="fun_1">Fun & Upbeat</option>
    <option value="mysterious_1">Mysterious Vibe</option>
    <option value="tech_explained_1">Tech Explained</option>
  </FormSelect>
);

const Voice = ({ value, onChange, error }) => (
  <FormSelect label="Narration Voice" name="voiceSelection" value={value} onChange={onChange} error={error}>
    <option value="openai_alloy">Alloy (Neutral)</option>
    <option value="openai_ash">Ash (Male)</option>
    <option value="openai_ballad">Ballad (Female)</option>
    <option value="openai_coral">Coral (Male)</option>
    <option value="openai_echo">Echo (Female)</option>
    <option value="openai_fable">Fable (Female)</option>
    <option value="openai_onyx">Onyx (Male)</option>
    <option value="openai_nova">Nova (Female)</option>
    <option value="openai_sage">Sage (Male)</option>
    <option value="openai_shimmer">Shimmer (Female)</option>
    <option value="openai_verse">Verse (Neutral)</option>
  </FormSelect>
);

const Theme = ({ value, onChange, error }) => (
    <FormSelect label="Reddit Theme" name="theme" value={value} onChange={onChange} error={error}>
        <option value="dark">Dark</option>
        <option value="light">Light</option>
    </FormSelect>
);


// Attach the sub-components as properties of the main component
VideoOptions.BackgroundVideo = BackgroundVideo;
VideoOptions.Music = Music;
VideoOptions.Voice = Voice;
VideoOptions.Theme = Theme;

export { VideoOptions };
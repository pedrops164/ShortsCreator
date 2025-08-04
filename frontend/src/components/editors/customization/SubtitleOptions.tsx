'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { Separator } from '@/components/ui/separator';

// --- Type Definition ---
// This should match the structure in the `CharacterExplainsParams` type
export interface SubtitleSettings {
  show: boolean;
  font: string;
  color: string;
  position: string;
}

// --- Prop Definition ---
interface SubtitleOptionsProps {
  value: SubtitleSettings;
  onChange: (field: keyof SubtitleSettings, value: any) => void;
  hasErrors?: boolean; 
}

export function SubtitleOptions({ value, onChange, hasErrors }: SubtitleOptionsProps) {
  return (
    <Card className={hasErrors ? 'border-destructive' : ''}>
        <CardHeader>
            <CardTitle>Subtitle Customization</CardTitle>
            <CardDescription>Adjust the appearance of the text captions.</CardDescription>
        </CardHeader>
        <CardContent>
            <div className="space-y-4">
                <div className="flex items-center justify-between">
                    <Label htmlFor="show-subtitles" className="font-semibold">
                    Enable Subtitles
                    </Label>
                    <Switch
                        id="show-subtitles"
                        checked={value.show}
                        onCheckedChange={(checked) => onChange('show', checked)}
                    />
                </div>
                {value.show && (
                <>
                    <Separator />
                    <div className="space-y-4 pt-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <Label className="text-xs text-muted-foreground">Font</Label>
                                <Select value={value.font} onValueChange={(font) => onChange('font', font)}>
                                    <SelectTrigger><SelectValue /></SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="Arial">Arial</SelectItem>
                                        <SelectItem value="Verdana">Verdana</SelectItem>
                                        <SelectItem value="The Bold Font">The Bold Font</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            <div>
                                <Label className="text-xs text-muted-foreground">Position</Label>
                                <Select value={value.position} onValueChange={(pos) => onChange('position', pos)}>
                                    <SelectTrigger><SelectValue /></SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="top">Top</SelectItem>
                                        <SelectItem value="center">Center</SelectItem>
                                        <SelectItem value="bottom">Bottom</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                        <div>
                            <Label className="text-xs text-muted-foreground">Text Color</Label>
                            <div className="flex items-center space-x-2">
                            <Input
                                type="color"
                                value={value.color}
                                onChange={(e) => onChange('color', e.target.value)}
                                className="p-1 h-10 w-10"
                            />
                            <Input
                                value={value.color.toUpperCase()}
                                onChange={(e) => onChange('color', e.target.value)}
                                className="w-24 bg-muted border"
                            />
                            </div>
                        </div>
                    </div>
                </>
                )}
            </div>
        </CardContent>
    </Card>
  );
}
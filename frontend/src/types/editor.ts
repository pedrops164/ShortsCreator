
// This handle allows the parent editor page to call functions on the child editor component.
export interface EditorHandle {
  // Runs validation and returns the latest form data.
  // Returns null if validation fails.
  getValidatedData: () => Record<string, unknown> | null;
}
// groups.ts
export interface SceneGroup {
    label: string
    sceneIds: string[]
}

export const groups: SceneGroup[] = [
    {
        label: "Person Supers",
        sceneIds: ["LeftPersonSuper", "RightPersonSuper"]
    },
    {
        label: "Theme",
        sceneIds: ["ThemeSuper", "Location"]
    }
]
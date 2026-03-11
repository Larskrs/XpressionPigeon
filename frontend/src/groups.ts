// groups.ts
export interface SceneGroup {
    label: string
    sceneIds: string[]
    flip_enabled?: boolean   // if true, show a ⇄ swap button between the first two sceneIds
}

export const groups: SceneGroup[] = [
    {
        label: "Person Supers",
        sceneIds: ["LeftPersonSuper", "RightPersonSuper"],
        flip_enabled: true,
    },
    {
        label: "Theme",
        sceneIds: ["ThemeSuper", "Location"],
    },
]
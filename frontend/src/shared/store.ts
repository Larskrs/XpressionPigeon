// store.ts - fix setValues to handle flat map from server
import { reactive } from "vue"

export const store = reactive({
    values: {} as Record<string, string>,

    setValues(data: Record<string, string>) {
        this.values = { ...data }
    },

    updateValue(key: string, value: string) {
        this.values[key] = value
    }
})
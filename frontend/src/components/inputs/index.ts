import BaseTextInput from "./BaseTextInput.vue"
import NumberInput from "./NumberInput.vue"
import BooleanInput from "./BooleanInput.vue"
import DurationInput from "./DurationInput.vue"
import TimeOfDayInput from "./TimeOfDayInput.vue"

export function resolveInput(type: string) {
    switch (type) {
        case "Number":
            return NumberInput
        case "Boolean":
            return BooleanInput
        case "Duration":
            return DurationInput
        case "Time":
            return TimeOfDayInput
        default:
            return BaseTextInput
    }
}
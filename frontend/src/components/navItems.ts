import FactCheckIcon from '@mui/icons-material/FactCheck'
import FitnessCenterIcon from '@mui/icons-material/FitnessCenter'
import PlaceIcon from '@mui/icons-material/Place'

// bottom navigation; each screen adds its entry here
export const NAV_ITEMS = [
  { path: '/checkin', label: 'Check-in', Icon: FactCheckIcon },
  { path: '/exercises', label: 'Exercises', Icon: FitnessCenterIcon },
  { path: '/locations', label: 'Locations', Icon: PlaceIcon },
]

import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import FactCheckIcon from '@mui/icons-material/FactCheck'
import FitnessCenterIcon from '@mui/icons-material/FitnessCenter'
import HistoryIcon from '@mui/icons-material/History'
import PlaceIcon from '@mui/icons-material/Place'
import TodayIcon from '@mui/icons-material/Today'

// bottom navigation; each screen adds its entry here
export const NAV_ITEMS = [
  { path: '/checkin', label: 'Check-in', Icon: FactCheckIcon },
  { path: '/today', label: 'Today', Icon: TodayIcon },
  { path: '/planner', label: 'Plan', Icon: CalendarMonthIcon },
  { path: '/history', label: 'History', Icon: HistoryIcon },
  { path: '/exercises', label: 'Exercises', Icon: FitnessCenterIcon },
  { path: '/locations', label: 'Locations', Icon: PlaceIcon },
]

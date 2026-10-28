import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import FitnessCenterIcon from '@mui/icons-material/FitnessCenter'
import HistoryIcon from '@mui/icons-material/History'
import PlaceIcon from '@mui/icons-material/Place'
import TodayIcon from '@mui/icons-material/Today'

// bottom navigation; each screen adds its entry here
export const NAV_ITEMS = [
  { path: '/today', label: 'Today', Icon: TodayIcon },
  { path: '/history', label: 'History', Icon: HistoryIcon },
  { path: '/planner', label: 'Plan', Icon: CalendarMonthIcon },
  { path: '/exercises', label: 'Exercises', Icon: FitnessCenterIcon },
  { path: '/locations', label: 'Locations', Icon: PlaceIcon },
]

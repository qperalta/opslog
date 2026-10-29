import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import FitnessCenterIcon from '@mui/icons-material/FitnessCenter'
import InsightsIcon from '@mui/icons-material/Insights'
import SettingsIcon from '@mui/icons-material/Settings'
import TodayIcon from '@mui/icons-material/Today'

// bottom navigation; each screen adds its entry here
export const NAV_ITEMS = [
  { path: '/today', label: 'Today', Icon: TodayIcon },
  { path: '/stats', label: 'Stats', Icon: InsightsIcon },
  { path: '/planner', label: 'Plan', Icon: CalendarMonthIcon },
  { path: '/exercises', label: 'Exercises', Icon: FitnessCenterIcon },
  { path: '/settings', label: 'Settings', Icon: SettingsIcon },
]

import { Box, Tab, Tabs } from '@mui/material'
import { useSearchParams } from 'react-router'
import { PageHeader } from '../components/PageHeader'
import { HistorySection } from '../history/HistorySection'
import { DashboardSection } from './DashboardSection'

const TABS = [
  { value: 'dashboard', label: 'Dashboard' },
  { value: 'history', label: 'History' },
]

/* the tab lives in the URL so a refresh or a link keeps it */
export function StatsPage() {
  const [params, setParams] = useSearchParams()
  const tab = TABS.some((item) => item.value === params.get('tab')) ? params.get('tab')! : 'dashboard'

  return (
    <>
      <PageHeader title="Stats" />
      <Tabs value={tab} onChange={(_, value: string) => setParams({ tab: value }, { replace: true })}
            variant="fullWidth" sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        {TABS.map((item) => (
          <Tab key={item.value} value={item.value} label={item.label} id={`tab-${item.value}`} aria-controls={`panel-${item.value}`} />
        ))}
      </Tabs>

      {/* both stay mounted so the history filters survive a tab switch */}
      <Box role="tabpanel" id="panel-dashboard" aria-labelledby="tab-dashboard" hidden={tab !== 'dashboard'}>
        <DashboardSection />
      </Box>
      <Box role="tabpanel" id="panel-history" aria-labelledby="tab-history" hidden={tab !== 'history'}>
        <HistorySection />
      </Box>
    </>
  )
}

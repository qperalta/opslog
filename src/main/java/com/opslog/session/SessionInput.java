package com.opslog.session;

import com.opslog.catalog.Exercise;
import com.opslog.checkin.TrainingTier;
import com.opslog.common.Require;
import com.opslog.location.Location;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/* everything a generator needs; no repositories */
public record SessionInput(List<PlannedExercise> planned, TrainingTier tier, Location location,
                           List<Exercise> catalog, Map<Long, BigDecimal> plannedLoads) {

    public SessionInput {
        Require.that(planned != null, "Planned exercises are required");
        Require.that(tier != null, "Training tier is required");
        Require.that(location != null, "Location is required");
        Require.that(catalog != null, "Catalog is required");
        planned = List.copyOf(planned);
        catalog = List.copyOf(catalog);
        plannedLoads = plannedLoads == null ? Map.of() : Map.copyOf(plannedLoads);
    }
}

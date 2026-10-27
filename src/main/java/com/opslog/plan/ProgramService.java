package com.opslog.plan;

import com.opslog.location.Location;
import com.opslog.location.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class ProgramService {

    private final ProgramTemplateRepository templates;
    private final UserProgramRepository programs;
    private final LocationService locationService;

    ProgramService(ProgramTemplateRepository templates, UserProgramRepository programs, LocationService locationService) {
        this.templates = templates;
        this.programs = programs;
        this.locationService = locationService;
    }

    @Transactional(readOnly = true)
    public List<ProgramTemplateResponse> templates() {
        return templates.findAllByOrderByNameAsc().stream().map(ProgramTemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserProgramResponse get(long userId) { return UserProgramResponse.from(find(userId)); }

    public UserProgramResponse choose(long userId, UserProgramRequest request) {
        ProgramTemplate template = templates.findById(request.templateId())
                .orElseThrow(() -> new ProgramTemplateNotFoundException(request.templateId()));
        Location location = request.locationId() == null ? null : locationService.findVisible(userId, request.locationId());
        UserProgram program = programs.findById(userId).orElseGet(() -> new UserProgram(userId));
        program.choose(template, location, LocalDate.now(ZoneOffset.UTC));
        return UserProgramResponse.from(programs.saveAndFlush(program));
    }

    /* used by the planner */
    @Transactional(readOnly = true)
    public UserProgram find(long userId) {
        return programs.findById(userId).orElseThrow(ProgramNotChosenException::new);
    }
}

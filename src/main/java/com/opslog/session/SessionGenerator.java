package com.opslog.session;

import com.opslog.plan.Actor;

public interface SessionGenerator {

    Actor source();

    SessionDraft generate(SessionInput input);
}

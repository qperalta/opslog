package com.opslog.notify;

import org.springframework.data.jpa.repository.JpaRepository;

/* keyed by user id, so findById is already scoped to the member */
public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
}

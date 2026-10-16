ALTER TABLE public.task_events
    ADD COLUMN from_priority character varying(255),
    ADD COLUMN to_priority character varying(255);

ALTER TABLE public.task_events
    DROP CONSTRAINT task_events_event_type_check;

ALTER TABLE public.task_events
    ADD CONSTRAINT task_events_event_type_check
    CHECK (event_type IN (
        'STATUS_PROGRESSED',
        'STATUS_ROLLBACK',
        'TASK_COMPLETED',
        'ASSIGNEE_CHANGED',
        'MOVED_TO_LIST',
        'PRIORITY_CHANGED'
    ));

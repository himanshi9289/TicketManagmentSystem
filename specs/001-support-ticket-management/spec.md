# Feature Specification: Support Ticket Management

**Feature Branch**: `001-support-ticket-management`

**Created**: 2026-09-20

**Status**: Draft

**Input**: User description: "Build a Support Ticket Management System. Users can create tickets (title, description, priority, assignee), list tickets, view ticket details, update title/description/priority/assignee, add comments, search tickets by keyword, and filter by status. Ticket status follows a strict workflow: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED, OPEN -> CANCELLED, IN_PROGRESS -> CANCELLED. No other transitions are allowed — specifically CLOSED, RESOLVED and CANCELLED can never go back to OPEN. Invalid transitions must be rejected with a clear error. All input must be validated, and the UI must show meaningful errors. Data must persist across application restarts."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create, list, and view a ticket (Priority: P1)

A user records a support issue as a ticket with a title, description, priority, and optional assignee, then finds that ticket in the list and opens it to read the full details. A newly created ticket is Open. The ticket is still there after the application is stopped and started again.

**Why this priority**: Nothing else in the system has value until a ticket can be recorded, found, and read back after a restart.

**Independent Test**: Create one ticket, confirm it appears in the list and on its detail view with the entered values and status Open, restart the application, and confirm the same ticket is unchanged.

**Acceptance Scenarios**:

1. **Given** no ticket with the chosen title, **When** the user submits a title, description, priority, and assignee, **Then** the system saves the ticket as Open and the detail view shows its reference, those values, status Open, created time, last updated time, and no comments.
2. **Given** the user is creating a ticket, **When** they leave the assignee empty, **Then** the ticket is saved as Open with no assignee and is shown as unassigned.
3. **Given** a ticket was saved, **When** the application is fully stopped and started again, **Then** the list and detail view show the same title, description, priority, assignee, and status.
4. **Given** the user is creating a ticket, **When** they omit the title, omit the description, or choose a priority outside Low, Medium, High, and Critical, **Then** the ticket is not saved and the screen names each invalid field and how to correct it, while keeping the other entered values.
5. **Given** several tickets were updated at different times, **When** the user opens the list, **Then** each row shows reference, title, status, priority, assignee or unassigned, and last updated time, with the most recently updated ticket first.

---

### User Story 2 - Move a ticket only along the allowed path (Priority: P1)

A user advances a ticket from Open to In Progress, then to Resolved, then to Closed. They may cancel a ticket only from Open or In Progress. Any other change of status is refused, the ticket stays where it was, and the screen explains why.

**Why this priority**: The status path is the business rule that keeps finished or cancelled work from being reopened by mistake.

**Independent Test**: Starting from a saved Open ticket, perform each allowed step and confirm the new status. Then attempt each forbidden step, including any return to Open, and confirm the status does not change and a clear error is shown.

**Acceptance Scenarios**:

1. **Given** a ticket is Open, **When** the user sets it to In Progress, **Then** the status becomes In Progress.
2. **Given** a ticket is Open, **When** the user sets it to Cancelled, **Then** the status becomes Cancelled.
3. **Given** a ticket is In Progress, **When** the user sets it to Resolved, **Then** the status becomes Resolved.
4. **Given** a ticket is In Progress, **When** the user sets it to Cancelled, **Then** the status becomes Cancelled.
5. **Given** a ticket is Resolved, **When** the user sets it to Closed, **Then** the status becomes Closed.
6. **Given** a ticket is Open, In Progress, Resolved, Closed, or Cancelled, **When** the user requests any status change that is not one of the five allowed steps above, **Then** the system rejects the change, the status stays the same, and the screen states the current status, the requested status, and that the ticket was not changed.
7. **Given** a ticket is Closed, Resolved, or Cancelled, **When** the user tries to set it to Open, **Then** the change is rejected and the status is unchanged.

---

### User Story 3 - Update ticket details (Priority: P2)

A user corrects the title, description, priority, or assignee of a ticket that is still Open, In Progress, or Resolved. Closed and Cancelled tickets cannot be edited. A detail update does not change the status.

**Why this priority**: People need to fix the record after it is created, but that is useful only once create and status rules exist.

**Independent Test**: Change each editable field on an Open ticket and confirm the detail view shows the new values and the same status. Repeat the attempt on a Closed ticket and confirm nothing changes and an error is shown.

**Acceptance Scenarios**:

1. **Given** a ticket is Open, In Progress, or Resolved, **When** the user changes the title, description, priority, assignee, or clears the assignee, **Then** the detail view shows the new values and the status is unchanged.
2. **Given** a ticket is Closed or Cancelled, **When** the user tries to change the title, description, priority, or assignee, **Then** the system rejects the change, the stored values stay the same, and the screen explains that a finished ticket cannot be edited.
3. **Given** the user is editing an editable ticket, **When** the new title, description, or priority is missing or invalid, **Then** nothing is saved and the screen names each invalid field and how to correct it.

---

### User Story 4 - Add comments (Priority: P2)

A user adds a comment to a ticket that is Open, In Progress, or Resolved. The comment shows the writer's name, the text, and when it was added. Comments appear oldest first. Closed and Cancelled tickets do not accept new comments.

**Why this priority**: Discussion belongs on the ticket, but the ticket itself must exist first.

**Independent Test**: Add two comments to an Open ticket and confirm both appear in order with name and text. Attempt a comment on a Cancelled ticket and confirm it is refused.

**Acceptance Scenarios**:

1. **Given** a ticket is Open, In Progress, or Resolved, **When** the user submits a comment with a name and text, **Then** the comment appears on the detail view with that name, text, and the time it was added.
2. **Given** a ticket already has comments, **When** the user adds another, **Then** comments are shown from oldest to newest.
3. **Given** a ticket is Closed or Cancelled, **When** the user tries to add a comment, **Then** the comment is not saved and the screen explains that a finished ticket cannot receive comments.
4. **Given** the user is adding a comment, **When** the name or the text is blank or too long, **Then** the comment is not saved and the screen names the invalid field and how to correct it.
5. **Given** comments were saved, **When** the application is restarted, **Then** the same comments appear on the ticket in the same order.

---

### User Story 5 - Search by keyword and filter by status (Priority: P2)

A user narrows the ticket list by a keyword, by a status, or by both. The keyword matches the title, description, or any comment, ignoring letter case. A status filter keeps only tickets in that status. Clearing both shows the full list again.

**Why this priority**: Search and filter matter once there are enough tickets that scanning the full list is slow. The system is still usable without them.

**Independent Test**: Create tickets with distinct titles, descriptions, comments, and statuses. Search for a word that appears only in a comment, filter to one status, then combine both, and confirm the list matches only the expected tickets.

**Acceptance Scenarios**:

1. **Given** tickets exist, **When** the user enters a keyword, **Then** the list shows only tickets whose title, description, or comment text contains that keyword, regardless of letter case.
2. **Given** tickets exist in more than one status, **When** the user filters by one status, **Then** the list shows only tickets in that status.
3. **Given** the user has entered both a keyword and a status, **When** the list is shown, **Then** every ticket shown matches both conditions.
4. **Given** no ticket matches, **When** the user searches or filters, **Then** the list is empty and the screen says no tickets match, without reporting an error.
5. **Given** a keyword or status filter is applied, **When** the user clears it, **Then** the list shows every ticket again.

---

### Edge Cases

- Title, description, comment name, or comment text that is only spaces is treated as empty and rejected.
- Title longer than 120 characters, description longer than 4,000 characters, comment text longer than 2,000 characters, or a name or assignee longer than 80 characters is rejected with a message that states the limit.
- Two tickets may share the same title. Each ticket has its own reference so users can tell them apart.
- Submitting the ticket's current status again does not change the ticket and is not an error.
- Open cannot jump to Resolved or Closed. In Progress cannot return to Open or jump to Closed. Resolved cannot go to In Progress, Cancelled, or Open. Closed and Cancelled cannot change to any other status.
- A keyword that matches only a comment still returns that ticket. A keyword that matches nothing returns an empty list, not an error.
- An unrecognized status filter is rejected with a message that lists the allowed statuses, rather than showing an empty list.
- If two people save different edits to the same ticket, the later save wins and the screen shows the values that were stored. The system does not merge the two edits.
- Stopping the application in the middle of a rejected save does not leave a partial ticket or a partial status change.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to create a ticket with a title, description, priority, and optional assignee. The new ticket's status MUST be Open. Users MUST NOT choose a different starting status.
- **FR-002**: Users MUST be able to list tickets and open one ticket to see its reference, title, description, priority, assignee or unassigned state, status, created time, last updated time, and comments.
- **FR-003**: Users MUST be able to change the title, description, priority, and assignee of a ticket that is Open, In Progress, or Resolved. Clearing the assignee MUST be allowed. A detail change MUST NOT change the status.
- **FR-004**: The system MUST reject changes to title, description, priority, or assignee when the ticket is Closed or Cancelled, and MUST leave the stored ticket unchanged.
- **FR-005**: Users MUST be able to add a comment with a writer name and text to a ticket that is Open, In Progress, or Resolved. Comments MUST show on the ticket from oldest to newest, each with its name, text, and time.
- **FR-006**: The system MUST reject a new comment when the ticket is Closed or Cancelled, and MUST NOT store that comment.
- **FR-007**: Users MUST be able to search tickets by a keyword. A match MUST be case-insensitive and MUST include the title, the description, and comment text. Users MUST be able to filter the list to one status. Keyword and status filter MUST be usable together, and each condition MUST apply. Users MUST be able to clear both and see every ticket.
- **FR-008**: The only allowed status changes MUST be Open to In Progress, Open to Cancelled, In Progress to Resolved, In Progress to Cancelled, and Resolved to Closed. Every other requested status change MUST be rejected. Closed, Resolved, and Cancelled MUST never return to Open.
- **FR-009**: A rejected status change MUST leave the ticket's status and other fields unchanged, and the screen MUST state the current status, the requested status, and that the ticket was not changed.
- **FR-010**: The system MUST validate every create, update, and comment before saving. Title is required and at most 120 characters. Description is required and at most 4,000 characters. Priority is required and MUST be one of Low, Medium, High, or Critical. Assignee is optional and at most 80 characters. Comment name is required and at most 80 characters. Comment text is required and at most 2,000 characters. Values that are only spaces MUST be treated as empty.
- **FR-011**: When input is invalid, the system MUST NOT save the change, and the screen MUST name each invalid field and the rule it broke, while keeping the user's other entered values so they can correct and resubmit.
- **FR-012**: Tickets, comments, and status MUST remain available with the same content after the application is stopped and started again. A rejected change MUST NOT be stored.
- **FR-013**: The list MUST show each ticket's reference, title, status, priority, assignee or unassigned state, and last updated time, with the most recently updated ticket first.
- **FR-014**: When no ticket matches a search or filter, the system MUST show an empty list and a plain statement that nothing matched. That outcome MUST NOT be presented as a failure.
- **FR-015**: Users MUST NOT be required to sign in. Any person using the application MUST be able to create, view, update, comment on, search, and change status of tickets, within the rules above.

### Key Entities

- **Ticket**: One support issue. It has a unique reference, title, description, priority (Low, Medium, High, or Critical), optional assignee, status (Open, In Progress, Resolved, Closed, or Cancelled), created time, and last updated time. It has zero or more comments.
- **Comment**: A note on one ticket. It has the writer's name, the text, and the time it was added. It cannot be edited or removed in this feature.
- **Status path**: The allowed steps between ticket statuses. Open may become In Progress or Cancelled. In Progress may become Resolved or Cancelled. Resolved may become Closed. No other step is allowed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create a ticket and see it in the list and on its detail view in under 1 minute.
- **SC-002**: In a review of status-change attempts, 100% of changes outside the five allowed steps are rejected, and in every rejected case the ticket status is the same as before the attempt.
- **SC-003**: After the application is restarted, 100% of previously saved tickets and comments are present with the same title, description, priority, assignee, status, and comment text.
- **SC-004**: A user can find a known ticket among 1,000 tickets by keyword or by status in under 10 seconds.
- **SC-005**: At least 95% of first attempts to submit invalid ticket or comment details produce a message that names the field to fix, without dropping the other values the user already entered.
- **SC-006**: A user can add a comment and see it on the ticket detail view in a single submission, in oldest-to-newest order.
- **SC-007**: A user who is shown a rejected status change can tell, from that message alone, what the status is now and what change was refused, without asking someone else.

## Assumptions

- People using the system are support staff in one shared workspace. This feature does not include accounts, sign-in, passwords, or separate requester and agent permissions. The assignee is a name typed by the user, not a chosen account.
- A new ticket always starts as Open. Priority is chosen by the user and has no automatic default. The allowed priorities are Low, Medium, High, and Critical.
- Title, description, priority, and assignee stay editable through Resolved, because Resolved is not the end of the path. Closed and Cancelled are finished: no detail edits and no new comments. Comments cannot be edited or deleted.
- Asking for the status the ticket already has is not a status change and is not an error.
- Search matches part of a word in the title, description, or comment text, and ignores letter case. An empty keyword means no keyword restriction. The status filter accepts only the five statuses.
- The list is ordered by last updated time, newest first. Two people editing the same ticket are not warned of each other; the later successful save replaces the earlier one.
- Length limits are 120 characters for the title, 4,000 for the description, 2,000 for a comment, and 80 for an assignee or comment name. These limits exist so validation messages can be specific.
- Out of scope for this feature: deleting a ticket, reopening a Closed, Resolved, or Cancelled ticket, file attachments, email or other notifications, dashboards, reporting, bulk edits, and assignment rules that pick an assignee automatically.
- No outside system is required. The application stores its own tickets and comments.

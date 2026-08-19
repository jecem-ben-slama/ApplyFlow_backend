package com.applyflow.tracker_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for DELETE /api/auth/account. The client sends whatever the user typed
 * verbatim — the server is the source of truth on whether it matches.
 * A blank/missing phrase is rejected by AccountDeletionService, which already
 * treats it as a non-match — no bean-validation dependency needed here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionRequestDto {
        private String confirmationPhrase;
}
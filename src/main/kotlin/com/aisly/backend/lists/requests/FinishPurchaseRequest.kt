package com.aisly.backend.lists.requests

import java.time.Instant
import java.util.UUID

data class FinishPurchaseRequest(val id: UUID? = null, val finishedAt: Instant? = null)

package ar.gov.entrerios.cge.concursos.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import ar.gov.entrerios.cge.concursos.core.database.entity.ConcursoEntity
import ar.gov.entrerios.cge.concursos.core.database.entity.MatchEntity

data class ConcursoWithMatches(
    @Embedded val concurso: ConcursoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "concursoId"
    )
    val matches: List<MatchEntity>
)

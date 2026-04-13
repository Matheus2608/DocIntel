package dev.matheus.repository;

import dev.matheus.entity.AgentStep;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AgentStepRepository implements PanacheRepositoryBase<AgentStep, String> {

    @Inject
    EntityManager entityManager;

    public List<AgentStep> findByMessageId(String messageId) {
        return list("messageId = ?1 order by sequenceIdx", messageId);
    }

    public int nextSequenceIdx(String messageId) {
        Integer max = entityManager.createQuery(
                "SELECT MAX(s.sequenceIdx) FROM AgentStep s WHERE s.messageId = :messageId",
                Integer.class
        ).setParameter("messageId", messageId).getSingleResult();
        return max == null ? 0 : max + 1;
    }

    public Map<String, Long> countByMessageIds(List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entityManager.createQuery(
                "SELECT s.messageId, COUNT(s) FROM AgentStep s WHERE s.messageId IN :ids GROUP BY s.messageId",
                Object[].class
        ).setParameter("ids", messageIds).getResultList();

        return rows.stream().collect(java.util.stream.Collectors.toMap(
                r -> (String) r[0],
                r -> (Long) r[1]
        ));
    }
}

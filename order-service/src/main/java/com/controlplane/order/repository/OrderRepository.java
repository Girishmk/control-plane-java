package com.controlplane.order.repository;

import com.controlplane.order.domain.Order;
import com.controlplane.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :before")
    List<Order> findStaleOrders(
            @Param("status") OrderStatus status,
            @Param("before") Instant before);

    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus WHERE o.id = :id AND o.status = :expectedStatus")
    int updateStatusConditional(
            @Param("id") UUID id,
            @Param("expectedStatus") OrderStatus expectedStatus,
            @Param("newStatus") OrderStatus newStatus);

    Optional<Order> findByIdAndUserId(UUID id, String userId);

    @Query(value = """
            SELECT DATE_TRUNC('hour', created_at) AS hour,
                   COUNT(*) AS total,
                   SUM(total_amount) AS revenue
            FROM orders
            WHERE created_at >= :since
            GROUP BY DATE_TRUNC('hour', created_at)
            ORDER BY hour DESC
            """, nativeQuery = true)
    List<Object[]> hourlyRevenueSince(@Param("since") Instant since);
}

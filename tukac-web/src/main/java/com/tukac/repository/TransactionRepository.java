package com.tukac.repository;

import com.tukac.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrderByTransactionDateDesc();
    List<Transaction> findByDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(String description, String category);
    List<Transaction> findByCreatedByOrderByTransactionDateDesc(Long createdBy);

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(String startDate, String endDate);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 'income' THEN t.amount ELSE -t.amount END), 0) FROM Transaction t")
    Double calculateBalance();
}

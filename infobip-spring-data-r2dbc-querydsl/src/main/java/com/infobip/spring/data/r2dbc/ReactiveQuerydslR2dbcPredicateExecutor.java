package com.infobip.spring.data.r2dbc;

import com.infobip.spring.data.common.Querydsl;
import com.querydsl.core.types.*;
import com.querydsl.sql.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QSort;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveQuerydslR2dbcPredicateExecutor<T> implements ReactiveQuerydslPredicateExecutor<T> {

    private final ConstructorExpression<T> constructorExpression;
    private final RelationalPath<T> path;
    private final SQLQueryFactory sqlQueryFactory;
    private final R2dbcEntityOperations entityOperations;
    private final Querydsl querydsl;

    public ReactiveQuerydslR2dbcPredicateExecutor(ConstructorExpression<T> constructorExpression,
                                                  RelationalPath<T> path,
                                                  SQLQueryFactory sqlQueryFactory,
                                                  R2dbcEntityOperations entityOperations,
                                                  Querydsl querydsl) {
        this.constructorExpression = constructorExpression;
        this.path = path;
        this.sqlQueryFactory = sqlQueryFactory;
        this.entityOperations = entityOperations;
        this.querydsl = querydsl;
    }

    @Override
    public Mono<T> findOne(Predicate predicate) {
        SQLQuery<T> sqlQuery = sqlQueryFactory.query()
                                              .select(entityProjection())
                                              .where(predicate)
                                              .from(path);
        return new QueryBuilder<>(entityOperations, sqlQuery).query().one();
    }

    @Override
    public Flux<T> findAll(Predicate predicate) {
        SQLQuery<T> query = sqlQueryFactory.query().select(entityProjection()).from(path).where(predicate);
        return new QueryBuilder<>(entityOperations, query).query().all();
    }

    @Override
    public Flux<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

        Assert.notNull(predicate, "Predicate must not be null!");
        Assert.notNull(orders, "Order specifiers must not be null!");

        return executeSorted(createQuery(predicate).select(constructorExpression), orders);
    }

    @Override
    public Flux<T> findAll(Predicate predicate, Sort sort) {

        Assert.notNull(predicate, "Predicate must not be null!");
        Assert.notNull(sort, "Sort must not be null!");

        return executeSorted(createQuery(predicate).select(constructorExpression), sort);
    }

    @Override
    public Flux<T> findAll(OrderSpecifier<?>... orders) {

        Assert.notNull(orders, "Order specifiers must not be null!");

        return executeSorted(createQuery(new Predicate[0]).select(constructorExpression), orders);
    }

    @Override
    public Mono<Long> count(Predicate predicate) {
        throw new UnsupportedOperationException();
//        SQLQuery<T> sqlQuery = sqlQueryFactory.query()
//                                              .select(entityProjection())
//                                              .where(predicate)
//                                              .from(path);
//        return new QueryBuilder<>(entityOperations, createQuery(predicate)).query().all().count();
    }

    @Override
    public Mono<Boolean> exists(Predicate predicate) {
        return count(predicate).map(result -> result > 0);
    }

    protected SQLQuery<?> createQuery(Predicate... predicate) {

        Assert.notNull(predicate, "Predicate must not be null!");

        return doCreateQuery(predicate);
    }

    protected SQLQuery<?> createCountQuery(@Nullable Predicate... predicate) {
        return doCreateQuery(predicate);
    }

    private SQLQuery<?> doCreateQuery(@Nullable Predicate... predicate) {

        SQLQuery<?> query = querydsl.createQuery(path);

        if (predicate != null) {
            query = query.where(predicate);
        }

        return query;
    }

    private Flux<T> executeSorted(SQLQuery<T> query, OrderSpecifier<?>... orders) {
        return executeSorted(query, new QSort(orders));
    }

    private Flux<T> executeSorted(SQLQuery<T> query, Sort sort) {
        SQLQuery<T> sqlQuery = querydsl.applySorting(sort, query);
        return new QueryBuilder<>(entityOperations, sqlQuery).query().all();
    }

    private Expression<T> entityProjection() {
        return constructorExpression;
    }
}
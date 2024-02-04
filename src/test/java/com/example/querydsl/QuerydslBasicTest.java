package com.example.querydsl;

import java.util.List;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;

import static com.querydsl.jpa.JPAExpressions.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @Transactional
class QuerydslBasicTest {

    @Autowired EntityManager entityManager;

    JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    void before() {

        jpaQueryFactory = new JPAQueryFactory(entityManager);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        Member member5 = new Member("member5", 50);
        Member member6 = new Member("member6", 50);
        Member member7 = new Member(null, 50);
        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);
        entityManager.persist(member5);
        entityManager.persist(member6);
        entityManager.persist(member7);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void startJPQL() {
        String queryString = "select m from Member m " +
                            "where username = :username";

        Member member = entityManager.createQuery(queryString, Member.class)
                        .setParameter("username", "member1")
                        .getSingleResult();

        assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {

        Member findMember = jpaQueryFactory
                        .select(member)
                        .from(member)
                        .where(member.username.eq("member1"))
                        .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() {
        Member member2 = jpaQueryFactory
            .selectFrom(member)
            .where(member.username.eq("member2")
                .and(member.age.eq(20)))
            .fetchOne();

        assertThat(member2.getUsername()).isEqualTo("member2");
        assertThat(member2.getAge()).isEqualTo(20);
    }

    @Test
    void searchAndParam() {
        Member member2 = jpaQueryFactory
            .selectFrom(member)
            .where(
                member.username.eq("member2"),
                member.age.eq(20)
            )
            .fetchOne();

        assertThat(member2.getUsername()).isEqualTo("member2");
        assertThat(member2.getAge()).isEqualTo(20);
    }

    @Test
    void resultFetch() {
        List<Member> fetch = jpaQueryFactory
            .selectFrom(member)
            .fetch();

//        Member fetchOne = jpaQueryFactory
//            .selectFrom(member)
//            .fetchOne();

        Member fetchFirst = jpaQueryFactory
            .selectFrom(member)
            .fetchFirst();

        QueryResults<Member> results = jpaQueryFactory
            .selectFrom(member)
            .fetchResults();

        long total = results.getTotal();

        List<Member> content = results.getResults();

        long memberCount = jpaQueryFactory
            .selectFrom(member)
            .fetchCount();
    }


    @Test
    void sort() {
        List<Member> members = jpaQueryFactory
            .selectFrom(member)
            .where(member.age.eq(50))
//            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .orderBy(member.age.desc(), member.username.asc().nullsFirst())
            .fetch();

        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void paging() {
        QueryResults<Member> members = jpaQueryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();
    }

    @Test
    void aggregation() {
        List<Tuple> fetch = jpaQueryFactory
            .select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
            )
            .from(member)
            .fetch();
    }


    @Test
    void group() {
        List<Tuple> fetch = jpaQueryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .innerJoin(member.team, team)
            .groupBy(team.name)
            .fetch();

        for (Tuple tuple : fetch) {
            System.out.println(tuple.toString());
        }
    }


    @Test
    void join() {
        List<Member> result = jpaQueryFactory
            .selectFrom(member)
            .innerJoin(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

        assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");
    }

    @Test
    void join_on_filtering() {

        List<Tuple> result = jpaQueryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team)
            .on(team.name.eq("teamA"))
//            .innerJoin(member.team, team)
//            .where(team.name.eq("teamA"))
            .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void subQuery1() {
        QMember memberSub = new QMember("memberSub");

        List<Member> members = jpaQueryFactory
            .selectFrom(member)
            .where(member.age.eq(
                select(memberSub.age.max())
                    .from(memberSub)
            )).fetch();

        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void subQuery2() {
        QMember memberSub = new QMember("memberSub");

        List<Member> members = jpaQueryFactory
            .selectFrom(member)
            .where(member.age.goe(
                select(memberSub.age.avg())
                    .from(memberSub)
            )).fetch();

        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void subQuery3() {
        QMember memberSub = new QMember("memberSub");

        List<Member> members = jpaQueryFactory
            .selectFrom(member)
            .where(member.age.in(
                select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))
            )).fetch();

        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> members = jpaQueryFactory
            .select(
                member.username,
                select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.id.eq(member.id))
            )
            .from(member)
            .fetch();

        for (Tuple tuple : members) {
            System.out.println("member1 = " + tuple);
        }
    }

    @Test
    void basicCase() {
        List<String> members = jpaQueryFactory
            .select(
                member.age
                    .when(10).then("열살")
                    .when(20).then("20대")
                    .when(30).then("30대")
                    .otherwise("40대")
            )
            .from(member)
            .where(member.age.lt(50))
            .fetch();

        for (String s : members) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() {
        List<String> members = jpaQueryFactory
            .select(
                new CaseBuilder()
                    .when(member.age.between(0,10)).then("0~10")
                    .when(member.age.between(10,19)).then("10대")
                    .when(member.age.between(20,29)).then("20대")
                    .when(member.age.between(30,39)).then("30대")
                    .otherwise("40대")
            )
            .from(member)
            .where(member.age.lt(50))
            .fetch();

        for (String s : members) {
            System.out.println("s = " + s);
        }
    }

}

/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.rewriting

import org.neo4j.cypher.internal.ast.AlterUser
import org.neo4j.cypher.internal.ast.CreateIndex
import org.neo4j.cypher.internal.ast.CreateNodeKeyConstraint
import org.neo4j.cypher.internal.ast.CreateNodePropertyExistenceConstraint
import org.neo4j.cypher.internal.ast.CreateRelationshipPropertyExistenceConstraint
import org.neo4j.cypher.internal.ast.CreateUniquePropertyConstraint
import org.neo4j.cypher.internal.ast.CreateUser
import org.neo4j.cypher.internal.ast.DbmsPrivilege
import org.neo4j.cypher.internal.ast.DefaultGraphScope
import org.neo4j.cypher.internal.ast.DenyPrivilege
import org.neo4j.cypher.internal.ast.DropConstraintOnName
import org.neo4j.cypher.internal.ast.DropIndexOnName
import org.neo4j.cypher.internal.ast.ExecuteAdminProcedureAction
import org.neo4j.cypher.internal.ast.ExecuteBoostedFunctionAction
import org.neo4j.cypher.internal.ast.ExecuteBoostedProcedureAction
import org.neo4j.cypher.internal.ast.ExecuteFunctionAction
import org.neo4j.cypher.internal.ast.ExecuteProcedureAction
import org.neo4j.cypher.internal.ast.GrantPrivilege
import org.neo4j.cypher.internal.ast.IfExistsDoNothing
import org.neo4j.cypher.internal.ast.RevokePrivilege
import org.neo4j.cypher.internal.ast.ShowCurrentUser
import org.neo4j.cypher.internal.ast.ShowPrivilegeCommands
import org.neo4j.cypher.internal.ast.ShowPrivileges
import org.neo4j.cypher.internal.ast.ShowRolesPrivileges
import org.neo4j.cypher.internal.ast.ShowUsersPrivileges
import org.neo4j.cypher.internal.ast.Statement
import org.neo4j.cypher.internal.ast.UseGraph
import org.neo4j.cypher.internal.expressions.ExistsSubClause
import org.neo4j.cypher.internal.util.CypherExceptionFactory

object Additions {

  // This is functionality that has been added in 4.0 and 4.1 and should not work when using CYPHER 3.5
  case object addedFeaturesIn4_x extends Additions {

    override def check(statement: Statement, cypherExceptionFactory: CypherExceptionFactory): Unit = statement.treeExists {

      case u: UseGraph =>
        throw cypherExceptionFactory.syntaxException("The USE clause is not supported in this Cypher version.", u.position)

      // CREATE INDEX [name] [IF NOT EXISTS] FOR (n:Label) ON (n.prop)
      case c: CreateIndex =>
        throw cypherExceptionFactory.syntaxException("Creating index using this syntax is not supported in this Cypher version.", c.position)

      // DROP INDEX name
      case d: DropIndexOnName =>
        throw cypherExceptionFactory.syntaxException("Dropping index by name is not supported in this Cypher version.", d.position)

      // CREATE CONSTRAINT name ON ... IS NODE KEY
      case c@CreateNodeKeyConstraint(_, _, _, Some(_), _, _, _) =>
        throw cypherExceptionFactory.syntaxException("Creating named node key constraint is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT [name] IF NOT EXISTS ON ... IS NODE KEY
      case c@CreateNodeKeyConstraint(_, _, _, _, IfExistsDoNothing, _, _) =>
        throw cypherExceptionFactory.syntaxException("Creating node key constraint using `IF NOT EXISTS` is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT name ON ... IS UNIQUE
      case c@CreateUniquePropertyConstraint(_, _, _, Some(_),_, _, _) =>
        throw cypherExceptionFactory.syntaxException("Creating named uniqueness constraint is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT [name] IF NOT EXISTS ON ... IS UNIQUE
      case c@CreateUniquePropertyConstraint(_, _, _, _, IfExistsDoNothing, _, _) =>
        throw cypherExceptionFactory.syntaxException("Creating uniqueness constraint using `IF NOT EXISTS` is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT name ON () ... EXISTS
      case c@CreateNodePropertyExistenceConstraint(_, _, _, Some(_),_, _) =>
        throw cypherExceptionFactory.syntaxException("Creating named node existence constraint is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT [name] IF NOT EXISTS ON () ... EXISTS
      case c@CreateNodePropertyExistenceConstraint(_, _, _, _, IfExistsDoNothing, _) =>
        throw cypherExceptionFactory.syntaxException("Creating node existence constraint using `IF NOT EXISTS` is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT name ON ()-[]-() ... EXISTS
      case c@CreateRelationshipPropertyExistenceConstraint(_, _, _, Some(_), _,_) =>
        throw cypherExceptionFactory.syntaxException("Creating named relationship existence constraint is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT [name] IF NOT EXISTS ON ()-[]-() ... EXISTS
      case c@CreateRelationshipPropertyExistenceConstraint(_, _, _, _, IfExistsDoNothing, _) =>
        throw cypherExceptionFactory.syntaxException("Creating relationship existence constraint using `IF NOT EXISTS` is not supported in this Cypher version.", c.position)

      // DROP CONSTRAINT name
      case d: DropConstraintOnName =>
        throw cypherExceptionFactory.syntaxException("Dropping constraint by name is not supported in this Cypher version.", d.position)

      case e: ExistsSubClause =>
        throw cypherExceptionFactory.syntaxException("Existential subquery is not supported in this Cypher version.", e.position)

      // Administration commands against system database are checked in CompilerFactory to cover all of them at once
    }
  }

  // This is functionality that has been added in 4.2 and should not work when using CYPHER 3.5 and CYPHER 4.1
  case object addedFeaturesIn4_2 extends Additions {

    override def check(statement: Statement, cypherExceptionFactory: CypherExceptionFactory): Unit = statement.treeExists {

      case s@ShowPrivilegeCommands(_, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("SHOW PRIVILEGES AS COMMANDS command is not supported in this Cypher version.", s.position)

      case c@CreateUser(_, true, _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("Creating a user with an encrypted password is not supported in this Cypher version.", c.position)

      case c@AlterUser(_, Some(true), _, _, _) =>
        throw cypherExceptionFactory.syntaxException("Updating a user with an encrypted password is not supported in this Cypher version.", c.position)

      // SHOW ROLE role1, role2 PRIVILEGES
      case s@ShowPrivileges(ShowRolesPrivileges(r), _, _) if r.size > 1 =>
        throw cypherExceptionFactory.syntaxException("Multiple roles in SHOW ROLE PRIVILEGE command is not supported in this Cypher version.", s.position)

      // SHOW USER user1, user2 PRIVILEGES
      case s@ShowPrivileges(ShowUsersPrivileges(u), _, _) if u.size > 1 =>
        throw cypherExceptionFactory.syntaxException("Multiple users in SHOW USER PRIVILEGE command is not supported in this Cypher version.", s.position)

      case d: DefaultGraphScope => throw cypherExceptionFactory.syntaxException("Default graph is not supported in this Cypher version.", d.position)

      // GRANT EXECUTE [BOOSTED|ADMIN] PROCEDURES ...
      case p@GrantPrivilege(DbmsPrivilege(ExecuteProcedureAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE PROCEDURE is not supported in this Cypher version.", p.position)
      case p@GrantPrivilege(DbmsPrivilege(ExecuteBoostedProcedureAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE BOOSTED PROCEDURE is not supported in this Cypher version.", p.position)
      case p@GrantPrivilege(DbmsPrivilege(ExecuteAdminProcedureAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE ADMIN PROCEDURES is not supported in this Cypher version.", p.position)

      // DENY EXECUTE [BOOSTED|ADMIN] PROCEDURES ...
      case p@DenyPrivilege(DbmsPrivilege(ExecuteProcedureAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE PROCEDURE is not supported in this Cypher version.", p.position)
      case p@DenyPrivilege(DbmsPrivilege(ExecuteBoostedProcedureAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE BOOSTED PROCEDURE is not supported in this Cypher version.", p.position)
      case p@DenyPrivilege(DbmsPrivilege(ExecuteAdminProcedureAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE ADMIN PROCEDURES is not supported in this Cypher version.", p.position)

      // REVOKE EXECUTE [BOOSTED|ADMIN] PROCEDURES ...
      case p@RevokePrivilege(DbmsPrivilege(ExecuteProcedureAction), _, _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE PROCEDURE is not supported in this Cypher version.", p.position)
      case p@RevokePrivilege(DbmsPrivilege(ExecuteBoostedProcedureAction), _, _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE BOOSTED PROCEDURE is not supported in this Cypher version.", p.position)
      case p@RevokePrivilege(DbmsPrivilege(ExecuteAdminProcedureAction), _, _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE ADMIN PROCEDURES is not supported in this Cypher version.", p.position)

      // GRANT EXECUTE [BOOSTED] [USER [DEFINED]] FUNCTION ...
      case p@GrantPrivilege(DbmsPrivilege(ExecuteFunctionAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE FUNCTION is not supported in this Cypher version.", p.position)
      case p@GrantPrivilege(DbmsPrivilege(ExecuteBoostedFunctionAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE BOOSTED FUNCTION is not supported in this Cypher version.", p.position)

      // DENY EXECUTE [BOOSTED] [USER [DEFINED]] FUNCTION ...
      case p@DenyPrivilege(DbmsPrivilege(ExecuteFunctionAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE FUNCTION is not supported in this Cypher version.", p.position)
      case p@DenyPrivilege(DbmsPrivilege(ExecuteBoostedFunctionAction), _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE BOOSTED FUNCTION is not supported in this Cypher version.", p.position)

      // REVOKE EXECUTE [BOOSTED] [USER [DEFINED]] FUNCTION ...
      case p@RevokePrivilege(DbmsPrivilege(ExecuteFunctionAction), _, _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE FUNCTION is not supported in this Cypher version.", p.position)
      case p@RevokePrivilege(DbmsPrivilege(ExecuteBoostedFunctionAction), _, _, _, _, _) =>
        throw cypherExceptionFactory.syntaxException("EXECUTE BOOSTED FUNCTION is not supported in this Cypher version.", p.position)

      // CREATE INDEX ... OPTIONS {...}
      case c@CreateIndex(_, _, _, _, _, options, _) if options.nonEmpty =>
        throw cypherExceptionFactory.syntaxException("Creating index with options is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT ... IS NODE KEY OPTIONS {...}
      case c@CreateNodeKeyConstraint(_, _, _, _, _, options, _) if options.nonEmpty =>
        throw cypherExceptionFactory.syntaxException("Creating node key constraint with options is not supported in this Cypher version.", c.position)

      // CREATE CONSTRAINT ... IS UNIQUE OPTIONS {...}
      case c@CreateUniquePropertyConstraint(_, _, _, _, _, options, _) if options.nonEmpty =>
        throw cypherExceptionFactory.syntaxException("Creating uniqueness constraint with options is not supported in this Cypher version.", c.position)

      // SHOW CURRENT USER
      case s: ShowCurrentUser => throw cypherExceptionFactory.syntaxException("SHOW CURRENT USER is not supported in this Cypher version.", s.position)
    }
  }
}

trait Additions extends {
  def check(statement: Statement, cypherExceptionFactory: CypherExceptionFactory): Unit = {}
}

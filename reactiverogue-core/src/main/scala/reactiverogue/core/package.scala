// Copyright 2011 Foursquare Labs Inc. All Rights Reserved.

package reactiverogue

package object core {

  type InitialState = Unordered with Unselected with Unlimited with Unskipped with HasNoOrClause with ShardKeyNotSpecified
  type OrderedState = Ordered with Unselected with Unlimited with Unskipped with HasNoOrClause with ShardKeyNotSpecified

  type SimpleQuery[M] = Query[M, M, InitialState]
  type OrderedQuery[M] = Query[M, M, OrderedState]

  trait Sharded
}

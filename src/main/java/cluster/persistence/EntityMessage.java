package cluster.persistence;

import akka.cluster.sharding.ShardRegion;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class EntityMessage {
    static class Amount implements Serializable {
        static final long serialVersionUID = 42L;
        final BigDecimal amount;
        private static final DecimalFormat df = new DecimalFormat(",##0.00");

        Amount(BigDecimal amount) {
            this.amount = amount;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), df.format(amount));
        }
    }

    static class EntityCommand implements Serializable {
        static final long serialVersionUID = 42L;
        final Entity.Id id;
        final Amount amount;

        private EntityCommand(Entity.Id id, Amount amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    static class DepositCommand extends EntityCommand {
        static final long serialVersionUID = 42L;
        DepositCommand(Entity.Id id, Amount amount) {
            super(id, amount);
        }

        @Override
        public String toString() {
            return String.format("%s[%s, %s]", getClass().getSimpleName(), id, amount);
        }
    }

    static class WithdrawalCommand extends EntityCommand {
        static final long serialVersionUID = 42L;
        WithdrawalCommand(Entity.Id id, Amount amount) {
            super(id, amount);
        }

        @Override
        public String toString() {
            return String.format("%s[%s, %s]", getClass().getSimpleName(), id, amount);
        }
    }

    static class EntityEvent implements Serializable {
        static final long serialVersionUID = 42L;
        final Entity.Id id;
        final Amount amount;
        final Instant time = Instant.now();

        private EntityEvent(Entity.Id id, Amount amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    static class DepositEvent extends EntityEvent {
        static final long serialVersionUID = 42L;
        DepositEvent(Entity.Id id, Amount amount) {
            super(id, amount);
        }

        DepositEvent(DepositCommand depositCommand) {
            this(depositCommand.id, depositCommand.amount);
        }

        @Override
        public String toString() {
            return String.format("%s[%s, %s, %s]", getClass().getSimpleName(), id, amount, time);
        }
    }

    static class WithdrawalEvent extends EntityEvent {
        static final long serialVersionUID = 42L;
        WithdrawalEvent(Entity.Id id, Amount amount) {
            super(id, amount);
        }

        WithdrawalEvent(WithdrawalCommand withdrawalCommand) {
            this(withdrawalCommand.id, withdrawalCommand.amount);
        }

        @Override
        public String toString() {
            return String.format("%s[%s, %s, %s]", getClass().getSimpleName(), id, amount, time);
        }
    }

    static class CommandAck implements Serializable {
        static final long serialVersionUID = 42L;
        final EntityEvent entityEvent;

        CommandAck(EntityEvent entityEvent) {
            this.entityEvent = entityEvent;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), entityEvent);
        }
    }

    static class Query implements Serializable {
        static final long serialVersionUID = 42L;
        final Entity.Id id;

        Query(Entity.Id id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), id);
        }
    }

    static class QueryAck implements Serializable {
        static final long serialVersionUID = 42L;
        final Entity entity;

        QueryAck(Entity entity) {
            this.entity = entity;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), entity);
        }
    }

    static class QueryAckNotFound implements Serializable {
        static final long serialVersionUID = 42L;
        final Entity.Id id;

        QueryAckNotFound(Entity.Id id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), id);
        }
    }

    static int numberOfEventTags = 20;

    static Set<String> eventTag(EntityCommand entityCommand) {
        return new HashSet<>(Collections.singletonList(String.format("%d", entityCommand.id.id.hashCode() % numberOfEventTags)));
    }

    static ShardRegion.MessageExtractor messageExtractor() {
        final int numberOfShards = 100;

        return new ShardRegion.MessageExtractor() {
            @Override
            public String shardId(Object message) {
                return extractShardIdFromCommand(message);
            }

            @Override
            public String entityId(Object message) {
                return extractEntityIdFromCommand(message);
            }

            @Override
            public Object entityMessage(Object message) {
                return message;
            }

            private String extractShardIdFromCommand(Object message) {
                if (message instanceof DepositCommand) {
                    return ((DepositCommand) message).id.id.hashCode() % numberOfShards + "";
                } else if (message instanceof WithdrawalCommand) {
                    return ((WithdrawalCommand) message).id.id.hashCode() % numberOfShards + "";
                } else if (message instanceof Query) {
                    return ((Query) message).id.id.hashCode() % numberOfShards + "";
                } else {
                    return null;
                }
            }

            private String extractEntityIdFromCommand(Object message) {
                if (message instanceof DepositCommand) {
                    return ((DepositCommand) message).id.id;
                } else if (message instanceof WithdrawalCommand) {
                    return ((WithdrawalCommand) message).id.id;
                } else if (message instanceof Query) {
                    return ((Query) message).id.id;
                } else {
                    return null;
                }
            }
        };
    }
}

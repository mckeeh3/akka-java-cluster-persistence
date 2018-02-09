package cluster.persistence;

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.cluster.sharding.ShardRegion;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.RecoveryCompleted;
import akka.persistence.journal.Tagged;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

class EntityPersistenceActor extends AbstractPersistentActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private Entity entity;
    private final FiniteDuration receiveTimeout = Duration.create(60, TimeUnit.SECONDS);

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(EntityMessage.DepositEvent.class, this::depositRecover)
                .match(EntityMessage.WithdrawalEvent.class, this::withdrawalRecover)
                .match(RecoveryCompleted.class, c -> recoveryCompleted())
                .match(EntityMessage.DepositCommand.class, this::deposit)
                .match(EntityMessage.WithdrawalCommand.class, this::withdrawal)
                .match(EntityMessage.Query.class, this::query)
                .build();
    }

    private void depositRecover(EntityMessage.DepositEvent depositEvent) {
        log.info("Recover {}", depositEvent);
        update(depositEvent);
    }

    private void withdrawalRecover(EntityMessage.WithdrawalEvent withdrawalEvent) {
        log.info("Recover {}", withdrawalEvent);
        update(withdrawalEvent);
    }

    private void recoveryCompleted() {
        log.debug("Recovery completed {}", entity);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(EntityMessage.DepositCommand.class, this::deposit)
                .match(EntityMessage.WithdrawalCommand.class, this::withdrawal)
                .match(EntityMessage.Query.class, this::query)
                .matchEquals(ReceiveTimeout.getInstance(), t -> passivate())
                .build();
    }

    private void deposit(EntityMessage.DepositCommand depositCommand) {
        persist(tagCommand(depositCommand), this::handleDeposit);
    }

    private void handleDeposit(Tagged taggedEvent) {
        if (taggedEvent.payload() instanceof EntityMessage.DepositEvent) {
            EntityMessage.DepositEvent depositEvent = (EntityMessage.DepositEvent) taggedEvent.payload();
            update(depositEvent);
            log.info("Deposit {} {} {}", entity, depositEvent, getSender());
            getSender().tell(new EntityMessage.CommandAck(depositEvent), getSelf());
        }
    }

    private void withdrawal(EntityMessage.WithdrawalCommand withdrawalCommand) {
        persist(tagCommand(withdrawalCommand), this::handleWithdrawal);
    }

    private void handleWithdrawal(Tagged taggedEvent) {
        if (taggedEvent.payload() instanceof EntityMessage.WithdrawalEvent) {
            EntityMessage.WithdrawalEvent withdrawalEvent = (EntityMessage.WithdrawalEvent) taggedEvent.payload();
            update(withdrawalEvent);
            log.info("Withdrawal {} {} {}", entity, withdrawalEvent, getSender());
            getSender().tell(new EntityMessage.CommandAck(withdrawalEvent), getSelf());
        }
    }

    private static Tagged tagCommand(EntityMessage.DepositCommand depositCommand) {
        return new Tagged(new EntityMessage.DepositEvent(depositCommand), EntityMessage.eventTag(depositCommand));
    }

    private static Tagged tagCommand(EntityMessage.WithdrawalCommand withdrawalCommand) {
        return new Tagged(new EntityMessage.WithdrawalEvent(withdrawalCommand), EntityMessage.eventTag(withdrawalCommand));
    }

    private void update(EntityMessage.DepositEvent depositEvent) {
        entity = entity == null
                ? Entity.deposit(depositEvent.id.id, depositEvent.amount.amount)
                : Entity.deposit(entity, depositEvent.amount.amount);
    }

    private void update(EntityMessage.WithdrawalEvent withdrawalEvent) {
        entity = entity == null
                ? Entity.withdrawal(withdrawalEvent.id.id, withdrawalEvent.amount.amount)
                : Entity.withdrawal(entity, withdrawalEvent.amount.amount);
    }

    private void query(EntityMessage.Query query) {
        if (entity == null) {
            getSender().tell(new EntityMessage.QueryAckNotFound(query.id), getSelf());
        } else {
            getSender().tell(new EntityMessage.QueryAck(entity), getSelf());
        }
    }

    private void passivate() {
        getContext().getParent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());
    }

    @Override
    public String persistenceId() {
        return entity == null ? getSelf().path().name() : entity.id.id;
    }

    @Override
    public void preStart() {
        log.info("Start");
        getContext().setReceiveTimeout(receiveTimeout);
    }

    @Override
    public void postStop() {
        log.info("Stop passivate {}", entity == null
                ? String.format("(entity %s not initialized)", getSelf().path().name())
                : entity.id);
    }

    static Props props() {
        return Props.create(EntityPersistenceActor.class);
    }
}

package cluster.persistence;

import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SerializationPerformanceBenchmark {
    public static void main(String[] args) {
        testJavaSerialization(1000);
        testJavaSerialization(10000);
        testJavaSerialization(100000);
        testJavaSerialization(1000000);

        testFstSerialization(1000);
        testFstSerialization(10000);
        testFstSerialization(100000);
        testFstSerialization(1000000);
    }

    private static void testJavaSerialization(int testSize) {
        System.out.println();
        testJavaSerializationDeserialize(testJavaSerializationSerialize(generateEntityCommands(testSize)));
    }

    private static List<Object> testJavaSerializationSerialize(List<EntityMessage.EntityCommand> entityCommands) {
        final long t1 = System.nanoTime();

        final List<Object> serializedEntityCommands = javaSerializeEntityCommands(entityCommands);

        final long t2 = System.nanoTime();

        final int count = serializedEntityCommands.size();
        final long elapsedNano = t2 - t1;
        final double elapsedMilli = elapsedNano / 1000000.0;
        final double elapsedSeconds = elapsedNano / 1000000000.0;

        System.out.printf("========== Java Serialize %,d ==========%n", count);
        System.out.printf("Test elapsed time %,dus%n", elapsedNano);
        System.out.printf("Serialized %,d objects in %.9fs%n", count, elapsedSeconds);
        System.out.printf("Serialization time %.3fms per object%n", elapsedMilli / count);

        return serializedEntityCommands;
    }

    private static void testJavaSerializationDeserialize(List<Object> serializedEntityCommands) {
        final long t1 = System.nanoTime();

        final List<EntityMessage.EntityCommand> entityCommands = serializedEntityCommands.stream()
                .map(SerializationPerformanceBenchmark::javaDeserialize)
                .collect(Collectors.toList());

        final long t2 = System.nanoTime();

        final int count = entityCommands.size();
        final long elapsedNano = t2 - t1;
        final double elapsedMilli = elapsedNano / 1000000.0;
        final double elapsedSeconds = elapsedNano / 1000000000.0;

        System.out.printf("========== Java Deserialize %,d ==========%n", count);
        System.out.printf("Test elapsed time %,dus%n", elapsedNano);
        System.out.printf("Deserialize %,d objects in %.9fs%n", count, elapsedSeconds);
        System.out.printf("Deserialization time %.3fms per object%n", elapsedMilli / count);
    }

    private static List<Object> javaSerializeEntityCommands(List<EntityMessage.EntityCommand> entityCommands) {
        return entityCommands.stream()
                .map(SerializationPerformanceBenchmark::javaSerializeEntityCommand)
                .collect(Collectors.toList());
    }

    private static final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private static final ObjectOutputStream out = objectOutputStream();

    private static ObjectOutputStream objectOutputStream() {
        try {
            return new ObjectOutputStream(bos);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create Java serializer.", e);
        }
    }

    private static byte[] javaSerializeEntityCommand(EntityMessage.EntityCommand entityCommand) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(entityCommand);
            out.flush();

            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private static EntityMessage.EntityCommand javaDeserialize(Object object) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) object);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (EntityMessage.EntityCommand) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void testFstSerialization(int testSize) {
        System.out.println();
        testFstSerializationDeserialize(testFstSerializationSerialize(generateEntityCommands(testSize)));
    }

    private static List<Object> testFstSerializationSerialize(List<EntityMessage.EntityCommand> entityCommands) {
        final long t1 = System.nanoTime();

        final List<Object> serializedEntityCommands = fstSerializeEntityCommands(entityCommands);

        final long t2 = System.nanoTime();

        final int count = serializedEntityCommands.size();
        final long elapsedNano = t2 - t1;
        final double elapsedMilli = elapsedNano / 1000000.0;
        final double elapsedSeconds = elapsedNano / 1000000000.0;

        System.out.printf("========== FST Serialize %,d ==========%n", count);
        System.out.printf("Test elapsed time %,dus%n", elapsedNano);
        System.out.printf("Serialized %,d objects in %.9fs%n", count, elapsedSeconds);
        System.out.printf("Serialization time %.3fms per object%n", elapsedMilli / count);

        return serializedEntityCommands;
    }

    private static void testFstSerializationDeserialize(List<Object> serializedEntityCommands) {
        final long t1 = System.nanoTime();

        final List<EntityMessage.EntityCommand> entityCommands = serializedEntityCommands.stream()
                .map(SerializationPerformanceBenchmark::fstDeserialize)
                .collect(Collectors.toList());

        final long t2 = System.nanoTime();

        final int count = entityCommands.size();
        final long elapsedNano = t2 - t1;
        final double elapsedMilli = elapsedNano / 1000000.0;
        final double elapsedSeconds = elapsedNano / 1000000000.0;

        System.out.printf("========== FST Deserialize %,d ==========%n", count);
        System.out.printf("Test elapsed time %,dus%n", elapsedNano);
        System.out.printf("Deserialize %,d objects in %.9fs%n", count, elapsedSeconds);
        System.out.printf("Deserialization time %.3fms per object%n", elapsedMilli / count);
    }

    private static List<Object> fstSerializeEntityCommands(List<EntityMessage.EntityCommand> entityCommands) {
        return entityCommands.stream()
                .map(SerializationPerformanceBenchmark::fstSerializeEntityCommand)
                .collect(Collectors.toList());
    }

    private static FSTConfiguration fstConfiguration = FSTConfiguration.createDefaultConfiguration();

    private static byte[] fstSerializeEntityCommand(EntityMessage.EntityCommand entityCommand) {
        return fstConfiguration.asByteArray(entityCommand);
    }

    private static EntityMessage.EntityCommand fstDeserialize(Object object) {
        return (EntityMessage.EntityCommand) fstConfiguration.asObject((byte[]) object);
    }

    private static List<EntityMessage.EntityCommand> generateEntityCommands(int count) {
        return Stream.generate(command())
                .limit(count)
                .collect(Collectors.toList());
    }

    private static Supplier<EntityMessage.EntityCommand> command() {
        return new Supplier<EntityMessage.EntityCommand>() {
            int i = 0;

            @Override
            public EntityMessage.EntityCommand get() {
                return command(++i);
            }
        };
    }

    private static EntityMessage.EntityCommand command(int index) {
        Entity.Id id = new Entity.Id("" + index);
        BigDecimal amount = Random.amount(-10000, 10000);

        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            return new EntityMessage.DepositCommand(id, new EntityMessage.Amount(amount));
        } else {
            return new EntityMessage.WithdrawalCommand(id, new EntityMessage.Amount(BigDecimal.valueOf(-1).multiply(amount)));
        }
    }
}

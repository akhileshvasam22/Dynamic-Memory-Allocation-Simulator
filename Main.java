import java.util.*;

class Partition {
    int partitionNumber;
    int size;
    boolean isFree;
    String processName;

    public Partition(int partitionNumber, int size) {
        this.partitionNumber = partitionNumber;
        this.size = size;
        this.isFree = true;
        this.processName = null;
    }

    public Partition(int partitionNumber, int size, boolean isFree, String processName) {
        this.partitionNumber = partitionNumber;
        this.size = size;
        this.isFree = isFree;
        this.processName = processName;
    }
}

public class Main {

    static int partitionCounter = 0;

    static void allocateMemory(List<Partition> memory, Map<String, Integer> processes, String strategy) {
        System.out.println("\n--- " + strategy.toUpperCase() + " Allocation ---\n");

        for (String proc : processes.keySet()) {
            boolean success = allocateProcessToMemory(memory, proc, processes.get(proc), strategy);
            if (!success) {
                System.out.println("Allocation failed for process " + proc + " of size " + processes.get(proc));
            }
        }

        printStatus(memory);
    }

    static boolean allocateProcessToMemory(List<Partition> memory, String proc, int size, String strategy) {
    int index = findPartitionIndex(memory, size, strategy);

    if (index == -1) {
        // Allocation failed, try compacting and then retry once
        System.out.println("Allocation failed for process " + proc + ", trying to compact memory and retry...");
        compactMemory(memory);
        index = findPartitionIndex(memory, size, strategy);
    }

    if (index != -1) {
        Partition part = memory.get(index);
        if (part.size > size) {
            // allocate with same partitionNumber (important for consistent output)
            memory.set(index, new Partition(part.partitionNumber, size, false, proc));
            memory.add(index + 1, new Partition(++partitionCounter, part.size - size));
        } else {
            part.isFree = false;
            part.processName = proc;
        }
        System.out.println("Process " + proc + " allocated to partition " + memory.get(index).partitionNumber);
        return true;
    }

    return false;
}

static int findPartitionIndex(List<Partition> memory, int size, String strategy) {
    int index = -1;

    switch (strategy.toLowerCase()) {
        case "first fit":
            for (int i = 0; i < memory.size(); i++) {
                if (memory.get(i).isFree && memory.get(i).size >= size) {
                    index = i;
                    break;
                }
            }
            break;

        case "best fit":
            int minSize = Integer.MAX_VALUE;
            for (int i = 0; i < memory.size(); i++) {
                Partition p = memory.get(i);
                int remainSize = p.size - size;
                if (p.isFree && remainSize >= 0 && remainSize < minSize) {
                    minSize = remainSize;
                    index = i;
                    if(minSize == 0){
                        break;
                    }
                }
            }
            break;

        case "worst fit":
            int maxSize = -1;
            for (int i = 0; i < memory.size(); i++) {
                Partition p = memory.get(i);
                int remainSize = p.size - size;
                if (p.isFree && remainSize >= 0 && remainSize > maxSize ) {
                    maxSize = remainSize;
                    index = i;
                }
            }
            break;
    }
    return index;
}


    static void printStatus(List<Partition> memory) {
        System.out.println("\nPartition Status:");
        for (Partition p : memory) {
            if (p.isFree) {
                System.out.println("Partition " + p.partitionNumber + ": Size=" + p.size + ", Free=true");
            } else {
                System.out.println("Partition " + p.partitionNumber + ": Size=" + p.size + ", Free=false, Process=" + p.processName);
            }
        }

        System.out.println("\nFree Partitions:");
        for (Partition p : memory) {
            if (p.isFree) {
                System.out.println("Free Partition " + p.partitionNumber + ": Size=" + p.size);
            }
        }

        int totalFree = 0;
        for (Partition p : memory) {
            if (p.isFree) totalFree += p.size;
        }
        System.out.println("\nExternal Fragmentation:");
        System.out.println("Total Free Memory: " + totalFree);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Partition> memory = new ArrayList<>();

        System.out.print("Enter total memory size: ");
        int totalMemory = scanner.nextInt();
        memory.add(new Partition(++partitionCounter, totalMemory));
        scanner.nextLine();

        System.out.print("Enter number of processes: ");
        int n = scanner.nextInt();
        scanner.nextLine();

        Map<String, Integer> processes = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            System.out.print("Enter process name: ");
            String name = scanner.nextLine();
            System.out.print("Enter size for process " + name + ": ");
            int size = scanner.nextInt();
            scanner.nextLine();
            processes.put(name, size);
        }

        System.out.print("Enter allocation strategy (First Fit / Best Fit / Worst Fit): ");
        String strategy = scanner.nextLine();

        allocateMemory(memory, processes, strategy);

        while (true) {
            System.out.print("\nChoose action: (d)eallocate, (c)ompact, (a)llocate new process, (e)xit: ");
            String action = scanner.nextLine().trim().toLowerCase();

            switch (action) {
                case "d":
                    System.out.print("Enter process name to deallocate: ");
                    String pname = scanner.nextLine();
                    deallocateProcess(memory, pname);
                    printStatus(memory);
                    break;
                case "c":
                    compactMemory(memory);
                    break;
                case "a":
                    System.out.print("Enter new process name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter size for process " + name + ": ");
                    int size = scanner.nextInt();
                    scanner.nextLine();
                    boolean success = allocateProcessToMemory(memory, name, size, strategy);
                    if (!success) System.out.println("Allocation failed for process " + name);
                    printStatus(memory);
                    break;
                case "e":
                    System.out.println("Exiting");
                    return;
                default:
                    System.out.println("Invalid option");
            }
        }
    }

    static void deallocateProcess(List<Partition> memory, String processName) {
        boolean found = false;
        for (Partition part : memory) {
            if (!part.isFree && part.processName.equals(processName)) {
                part.isFree = true;
                part.processName = null;
                found = true;
                System.out.println("Deallocated process " + processName);
                break;
            }
        }
        if (!found) {
            System.out.println("Process " + processName + " not found in memory");
        }
        mergeFreePartitions(memory);
    }

    static void compactMemory(List<Partition> memory) {
        List<Partition> compacted = new ArrayList<>();
        int totalFree = 0;

        for (Partition p : memory) {
            if (!p.isFree) {
                compacted.add(new Partition(++partitionCounter, p.size, false, p.processName));
            } else {
                totalFree += p.size;
            }
        }

        if (totalFree > 0) {
            compacted.add(new Partition(++partitionCounter, totalFree));
        }

        memory.clear();
        memory.addAll(compacted);

        System.out.println("Memory Compacted");
        printStatus(memory);
    }

    static void mergeFreePartitions(List<Partition> memory) {
        for (int i = 0; i < memory.size() - 1; ) {
            Partition curr = memory.get(i);
            Partition next = memory.get(i + 1);
            if (curr.isFree && next.isFree) {
                curr.size += next.size;
                memory.remove(i + 1);
            } else {
                i++;
            }
        }
    }
}
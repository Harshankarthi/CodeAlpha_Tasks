import java.util.ArrayList;
import java.util.Scanner;

class Student {
    private String name;
    private int grade;

    public Student(String name, int grade) {
        this.name = name;
        this.grade = grade;
    }

    public String getName() {
        return name;
    }

    public int getGrade() {
        return grade;
    }
}

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<Student> students = new ArrayList<>();

        while (true) {
            System.out.println("\n===== STUDENT GRADE TRACKER =====");
            System.out.println("1. Add Student");
            System.out.println("2. View Summary Report");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); 

            if (choice == 1) {
                System.out.print("Enter student name: ");
                String name = scanner.nextLine();

                System.out.print("Enter student grade: ");
                int grade = scanner.nextInt();

                students.add(new Student(name, grade));
                System.out.println("Student added successfully!");

            } else if (choice == 2) {
                if (students.isEmpty()) {
                    System.out.println("No students available!");
                    continue;
                }

                int total = 0;
                int highest = Integer.MIN_VALUE;
                int lowest = Integer.MAX_VALUE;

                System.out.println("\n===== STUDENT REPORT =====");
                for (Student s : students) {
                    System.out.println("Name: " + s.getName() + " | Grade: " + s.getGrade());
                    total += s.getGrade();
                    if (s.getGrade() > highest) highest = s.getGrade();
                    if (s.getGrade() < lowest) lowest = s.getGrade();
                }

                double average = (double) total / students.size();

                System.out.println("\n--- Statistics ---");
                System.out.println("Average Grade: " + average);
                System.out.println("Highest Grade: " + highest);
                System.out.println("Lowest Grade: " + lowest);

            } else if (choice == 3) {
                System.out.println("Exiting program...");
                break;
            } else {
                System.out.println("Invalid choice! Try again.");
            }
        }

        scanner.close();
    }
}

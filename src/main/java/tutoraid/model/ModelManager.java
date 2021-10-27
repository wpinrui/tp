package tutoraid.model;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import tutoraid.commons.core.GuiSettings;
import tutoraid.commons.core.LogsCenter;
import tutoraid.commons.util.CollectionUtil;
import tutoraid.model.lesson.Lesson;
import tutoraid.model.student.Student;
import tutoraid.ui.UiManager;

/**
 * Represents the in-memory model of the student book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final StudentBook studentBook;
    private final LessonBook lessonBook;
    private final UserPrefs userPrefs;
    private final FilteredList<Student> filteredStudents;
    private final FilteredList<Lesson> filteredLessons;

    /**
     * Initializes a ModelManager with the given studentBook, lessonBook and userPrefs.
     */
    public ModelManager(ReadOnlyStudentBook studentBook, ReadOnlyLessonBook lessonBook, ReadOnlyUserPrefs userPrefs) {
        super();
        CollectionUtil.requireAllNonNull(studentBook, lessonBook, userPrefs);

        logger.fine("Initializing with student book: " + studentBook
                + ", lesson book: " + lessonBook + " and user prefs " + userPrefs);

        this.studentBook = new StudentBook(studentBook);
        this.lessonBook = new LessonBook(lessonBook);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredStudents = new FilteredList<>(this.studentBook.getStudentList());
        filteredLessons = new FilteredList<>(this.lessonBook.getLessonList());
    }

    public ModelManager() {
        this(new StudentBook(), new LessonBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public void setUserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        this.userPrefs.resetData(userPrefs);
    }

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public Path getStudentBookFilePath() {
        return userPrefs.getStudentBookFilePath();
    }

    @Override
    public void setStudentBookFilePath(Path studentBookFilePath) {
        requireNonNull(studentBookFilePath);
        userPrefs.setStudentBookFilePath(studentBookFilePath);
    }

    @Override
    public Path getLessonBookFilePath() {
        return userPrefs.getLessonBookFilePath();
    }

    @Override
    public void setLessonBookFilePath(Path lessonBookFilePath) {
        requireNonNull(lessonBookFilePath);
        userPrefs.setLessonBookFilePath(lessonBookFilePath);
    }

    //=========== StudentBook ================================================================================

    @Override
    public void setStudentBook(ReadOnlyStudentBook studentBook) {
        this.studentBook.resetData(studentBook);
    }

    @Override
    public ReadOnlyStudentBook getStudentBook() {
        return studentBook;
    }

    @Override
    public boolean hasStudent(Student student) {
        requireNonNull(student);
        return studentBook.hasStudent(student);
    }

    @Override
    public void deleteStudent(Student target) {
        studentBook.removeStudent(target);
    }

    @Override
    public void addStudent(Student student) {
        studentBook.addStudent(student);
        updateFilteredStudentList(PREDICATE_SHOW_ALL_STUDENTS);
    }

    @Override
    public void setStudent(Student target, Student editedStudent) {
        CollectionUtil.requireAllNonNull(target, editedStudent);

        studentBook.setStudent(target, editedStudent);
    }

    @Override
    public void viewStudent(Student targetStudent) {
        requireNonNull(targetStudent);
        filteredStudents.setPredicate(student -> student.equals(targetStudent));
        filteredLessons.setPredicate(lesson ->
                targetStudent.getLessons().getAllLessonNamesAsStringArrayList().contains(lesson.nameAsString()));
        UiManager.showViewWindow();
    }

    @Override
    public void viewList(boolean viewAll) {
        if (viewAll) {
            UiManager.showViewWindow();
        } else {
            UiManager.hideViewWindow();
        }
    }

    @Override
    public void deleteLessonFromStudents(Lesson lesson) {
        for (Student student : filteredStudents) {
            if (student.hasLesson(lesson)) {
                student.getLessons().deleteLesson(lesson);
            }
        }
        studentBook.refreshStudentBook();
    }

    //=========== LessonBook ================================================================================

    @Override
    public void setLessonBook(ReadOnlyLessonBook lessonBook) {
        this.lessonBook.resetData(lessonBook);
    }

    @Override
    public ReadOnlyLessonBook getLessonBook() {
        return lessonBook;
    }

    @Override
    public boolean hasLesson(Lesson lesson) {
        requireNonNull(lesson);
        return lessonBook.hasLesson(lesson);
    }

    @Override
    public void deleteLesson(Lesson target) {
        lessonBook.removeLesson(target);
    }

    @Override
    public void addLesson(Lesson lesson) {
        lessonBook.addLesson(lesson);
        updateFilteredLessonList(PREDICATE_SHOW_ALL_LESSONS);
    }

    @Override
    public void setLesson(Lesson target, Lesson editedLesson) {
        CollectionUtil.requireAllNonNull(target, editedLesson);
        lessonBook.setLesson(target, editedLesson);
    }

    @Override
    public void viewLesson(Lesson targetLesson) {
        requireNonNull(targetLesson);
        filteredLessons.setPredicate(lesson -> lesson.equals(targetLesson));
        filteredStudents.setPredicate(student ->
                targetLesson.getStudents().getAllStudentNamesAsStringArrayList().contains(student.toNameString()));
        UiManager.showViewWindow();
    }

    @Override
    public void deleteStudentFromLessons(Student student) {
        for (Lesson lesson : filteredLessons) {
            if (lesson.containsStudent(student)) {
                lesson.removeStudent(student);
            }
        }
        lessonBook.refreshLessonBook();
    }

    //=========== Filtered Student List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Student} backed by the internal list of
     * {@code versionedStudentBook}
     */
    @Override
    public ObservableList<Student> getFilteredStudentList() {
        return filteredStudents;
    }

    @Override
    public void updateFilteredStudentList(Predicate<Student> predicate) {
        requireNonNull(predicate);
        studentBook.refreshStudentBook();
        filteredStudents.setPredicate(predicate);
    }

    //=========== Filtered Lesson List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Lesson} backed by the internal list of
     * {@code versionedLessonBook}
     */
    @Override
    public ObservableList<Lesson> getFilteredLessonList() {
        return filteredLessons;
    }

    @Override
    public void updateFilteredLessonList(Predicate<Lesson> predicate) {
        requireNonNull(predicate);
        lessonBook.refreshLessonBook();
        filteredLessons.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object obj) {
        // short circuit if same object
        if (obj == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(obj instanceof ModelManager)) {
            return false;
        }

        // state check
        ModelManager other = (ModelManager) obj;
        return studentBook.equals(other.studentBook)
                && lessonBook.equals(other.lessonBook)
                && userPrefs.equals(other.userPrefs)
                && filteredStudents.equals(other.filteredStudents)
                && filteredLessons.equals(other.filteredLessons);
    }

}

package com.example.book.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.book.model.Book;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

@Service
public class BookService {

    private final DatabaseReference dbRef = FirebaseDatabase
        .getInstance()
        .getReference("books");

    public void saveBook(Book book) {
        if (book.getId() == null || book.getId().isEmpty()) {
            book.setId(UUID.randomUUID().toString());
            book.setTimestamp(System.currentTimeMillis());
        }
        dbRef.child(book.getId()).setValueAsync(book);
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        dbRef.orderByChild("timestamp")
            .addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Book book = data.getValue(Book.class);
                    books.add(book);
                }
                latch.countDown();
            }

            public void onCancelled(DatabaseError error) {
                System.err.println("Error reading books: " + error.getMessage());
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return books;
    }

    public List<Book> searchBooks(String query) {
        List<Book> allBooks = getAllBooks(); // from Firebase
        String lower = query.toLowerCase();

        return allBooks.stream()
                .filter(book -> 
                    (book.getTitle() != null && book.getTitle().toLowerCase().contains(lower)) ||
                    (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lower))
                )
                .collect(Collectors.toList());
    }


    public Book getBookById(String id) {
        final Book[] book = {null};
        CountDownLatch latch = new CountDownLatch(1);

        dbRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot snapshot) {
                book[0] = snapshot.getValue(Book.class);
                latch.countDown();
            }

            public void onCancelled(DatabaseError error) {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return book[0];
    }

    public void deleteBook(String id) {
        dbRef.child(id).removeValueAsync();
    }
}

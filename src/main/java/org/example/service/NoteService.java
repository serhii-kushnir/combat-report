package org.example.service;

import org.example.entity.Note;
import org.example.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NoteService {

    private final NoteRepository repository;

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public List<Note> getAllActive() {
        return repository.findByArchivedFalseOrderByPinnedDescUpdatedAtDesc();
    }

    public List<Note> getAllArchived() {
        return repository.findByArchivedTrueOrderByUpdatedAtDesc();
    }

    public Optional<Note> getById(Long id) {
        return repository.findById(id);
    }

    public Note save(Note note) {
        if (note.getId() == null) {
            note.setCreatedAt(LocalDateTime.now());
        }
        note.setUpdatedAt(LocalDateTime.now());
        return repository.save(note);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void archive(Long id) {
        repository.findById(id).ifPresent(note -> {
            note.setArchived(true);
            note.setUpdatedAt(LocalDateTime.now());
            repository.save(note);
        });
    }

    @Transactional
    public void unarchive(Long id) {
        repository.findById(id).ifPresent(note -> {
            note.setArchived(false);
            note.setUpdatedAt(LocalDateTime.now());
            repository.save(note);
        });
    }

    @Transactional
    public void togglePin(Long id) {
        repository.findById(id).ifPresent(note -> {
            note.setPinned(!note.isPinned());
            note.setUpdatedAt(LocalDateTime.now());
            repository.save(note);
        });
    }

    public List<Note> search(String query) {
        if (query == null || query.isBlank()) {
            return getAllActive();
        }
        return repository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseAndArchivedFalse(
                query, query);
    }
}
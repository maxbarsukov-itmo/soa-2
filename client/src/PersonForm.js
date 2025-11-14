import React, { useState, useEffect } from 'react';
import { Form, Button, Row, Col } from 'react-bootstrap';
import axios from 'axios';
import ErrorHandler from './ErrorHandler';

const PEOPLE_SERVICE_URL = 'http://localhost:8765/api/v1';

const PersonForm = ({ person, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    name: '',
    coordinates: { x: null, y: null },
    height: null,
    eyeColor: '',
    hairColor: '',
    nationality: '',
    location: { x: null, y: null, z: null, name: '' }
  });
  const [originalData, setOriginalData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (person) {
      const data = {
        name: person.name || '',
        coordinates: { x: person.coordinates?.x || null, y: person.coordinates?.y || null },
        height: person.height || null,
        eyeColor: person.eyeColor || '',
        hairColor: person.hairColor || null,
        nationality: person.nationality || null,
        location: {
          x: person.location?.x || null,
          y: person.location?.y || null,
          z: person.location?.z || null,
          name: person.location?.name || null
        }
      };
      setFormData(data);
      setOriginalData({ ...data, id: person.id });
    } else {
      const emptyData = {
        name: '',
        coordinates: { x: null, y: null },
        height: null,
        eyeColor: '',
        hairColor: null,
        nationality: null,
        location: { x: null, y: null, z: null, name: null }
      };
      setFormData(emptyData);
      setOriginalData(null);
    }
  }, [person]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name.startsWith('coordinates.')) {
      const coordField = name.split('.')[1];
      setFormData(prev => ({
        ...prev,
        coordinates: { ...prev.coordinates, [coordField]: value !== '' ? parseInt(value) : null }
      }));
    } else if (name.startsWith('location.')) {
      const locField = name.split('.')[1];
      setFormData(prev => ({
        ...prev,
        location: { ...prev.location, [locField]: value !== '' ? value : null }
      }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value !== '' ? value : null }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      if (person) { // Режим обновления (PATCH)
        const updates = {};
        // Сравниваем с originalData, чтобы отправить только изменённые поля
        if (originalData.name !== formData.name) updates.name = formData.name;
        if (originalData.height !== formData.height) updates.height = formData.height;
        if (originalData.eyeColor !== formData.eyeColor) updates.eyeColor = formData.eyeColor;
        if (originalData.hairColor !== formData.hairColor) updates.hairColor = formData.hairColor;
        if (originalData.nationality !== formData.nationality) updates.nationality = formData.nationality;

        if (JSON.stringify(originalData.coordinates) !== JSON.stringify(formData.coordinates)) {
          updates.coordinates = formData.coordinates;
        }
        if (JSON.stringify(originalData.location) !== JSON.stringify(formData.location)) {
          updates.location = formData.location;
        }

        if (Object.keys(updates).length === 0) {
             // Ничего не изменилось
             alert("No changes detected.");
             return;
        }

        await axios.patch(`${PEOPLE_SERVICE_URL}/people/${person.id}`, updates);
      } else { // Режим создания (POST)
        const dataToSubmit = { ...formData };
        // Убедимся, что null значения не превращаются в пустые строки
        if (dataToSubmit.height === '') dataToSubmit.height = null;
        if (dataToSubmit.nationality === '') dataToSubmit.nationality = null;
        if (dataToSubmit.hairColor === '') dataToSubmit.hairColor = null;
        if (dataToSubmit.location.name === '') dataToSubmit.location.name = null;

        await axios.post(`${PEOPLE_SERVICE_URL}/people`, dataToSubmit);
      }
      onSave();
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      {error && <ErrorHandler error={error} />}
      <Row>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="formName">
            <Form.Label>Name *</Form.Label>
            <Form.Control
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group className="mb-3" controlId="formHeight">
            <Form.Label>Height</Form.Label>
            <Form.Control
              type="number"
              step="0.01"
              name="height"
              value={formData.height ?? ''}
              onChange={handleChange}
            />
          </Form.Group>
        </Col>
      </Row>
      <Row>
        <Col md={4}>
          <Form.Group className="mb-3" controlId="formCoordX">
            <Form.Label>Coordinates X *</Form.Label>
            <Form.Control
              type="number"
              name="coordinates.x"
              value={formData.coordinates.x ?? ''}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group className="mb-3" controlId="formCoordY">
            <Form.Label>Coordinates Y *</Form.Label>
            <Form.Control
              type="number"
              name="coordinates.y"
              value={formData.coordinates.y ?? ''}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group className="mb-3" controlId="formEyeColor">
            <Form.Label>Eye Color *</Form.Label>
            <Form.Select
              name="eyeColor"
              value={formData.eyeColor}
              onChange={handleChange}
              required
            >
              <option value="">Select...</option>
              <option value="RED">Red</option>
              <option value="BLUE">Blue</option>
              <option value="YELLOW">Yellow</option>
              <option value="ORANGE">Orange</option>
            </Form.Select>
          </Form.Group>
        </Col>
      </Row>
      <Row>
        <Col md={4}>
          <Form.Group className="mb-3" controlId="formHairColor">
            <Form.Label>Hair Color</Form.Label>
            <Form.Select
              name="hairColor"
              value={formData.hairColor ?? ''}
              onChange={handleChange}
            >
              <option value="">Select...</option>
              <option value="GREEN">Green</option>
              <option value="RED">Red</option>
              <option value="YELLOW">Yellow</option>
              <option value="ORANGE">Orange</option>
              <option value="BROWN">Brown</option>
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group className="mb-3" controlId="formNationality">
            <Form.Label>Nationality</Form.Label>
            <Form.Select
              name="nationality"
              value={formData.nationality ?? ''}
              onChange={handleChange}
            >
              <option value="">Select...</option>
              <option value="CHINA">China</option>
              <option value="INDIA">India</option>
              <option value="ITALY">Italy</option>
              <option value="NORTH_KOREA">North Korea</option>
            </Form.Select>
          </Form.Group>
        </Col>
      </Row>
      <Row>
        <Col md={3}>
          <Form.Group className="mb-3" controlId="formLocX">
            <Form.Label>Location X *</Form.Label>
            <Form.Control
              type="number"
              name="location.x"
              value={formData.location.x ?? ''}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group className="mb-3" controlId="formLocY">
            <Form.Label>Location Y *</Form.Label>
            <Form.Control
              type="number"
              name="location.y"
              value={formData.location.y ?? ''}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group className="mb-3" controlId="formLocZ">
            <Form.Label>Location Z *</Form.Label>
            <Form.Control
              type="number"
              name="location.z"
              value={formData.location.z ?? ''}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group className="mb-3" controlId="formLocName">
            <Form.Label>Location Name</Form.Label>
            <Form.Control
              type="text"
              name="location.name"
              value={formData.location.name ?? ''}
              onChange={handleChange}
              maxLength="704"
            />
          </Form.Group>
        </Col>
      </Row>
      <Button variant="primary" type="submit">
        {person ? 'Update Person' : 'Create Person'}
      </Button>{' '}
      <Button variant="secondary" onClick={onCancel}>
        Cancel
      </Button>
    </Form>
  );
};

export default PersonForm;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.matrix;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.WritableComparable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Implementations of generic capabilities like sum of elements and dot products
 */
public abstract class AbstractVector implements Vector {

  /**
   * User-settable mapping between String labels and Integer indices. Marked
   * transient so that it will not be serialized with each vector instance.
   */
  private transient Map<String, Integer> bindings;
  protected String name;


  protected AbstractVector() {
  }

  protected AbstractVector(String name) {
    this.name = name;
  }

  /**
   * Subclasses must override to return an appropriately sparse or dense result
   * 
   * @param rows the row cardinality
   * @param columns the column cardinality
   * @return a Matrix
   */
  protected abstract Matrix matrixLike(int rows, int columns);

  /**
   * Returns an iterator for traversing the Vector, but not in any particular
   * order. The actual implementations may make some guarantees about the order
   * in which the vector is traversed. Otherwise, the traversal order is
   * undefined.
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public abstract java.util.Iterator<Vector.Element> iterator();

  @Override
  public Vector.Element getElement(int index) {
    return new Element(index);
  }

  public class Element implements Vector.Element {
    private final int ind;

    public Element(int ind) {
      this.ind = ind;
    }

    @Override
    public double get() {
      return getQuick(ind);
    }

    @Override
    public int index() {
      return ind;
    }

    @Override
    public void set(double value) {
      setQuick(ind, value);
    }
  }

  public abstract Vector clone();

  @Override
  public Vector divide(double x) {
    Vector result = clone();
    for (int i = 0; i < result.size(); i++)
      result.setQuick(i, getQuick(i) / x);
    return result;
  }

  @Override
  public double dot(Vector x) {
    if (size() != x.size())
      throw new CardinalityException();
    double result = 0;
    for (int i = 0; i < size(); i++)
      result += getQuick(i) * x.getQuick(i);
    return result;
  }

  @Override
  public double get(int index) {
    if (index >= 0 && index < size())
      return getQuick(index);
    else
      throw new IndexException();
  }

  @Override
  public Vector minus(Vector x) {
    if (size() != x.size())
      throw new CardinalityException();
    Vector result = clone();
    for (int i = 0; i < result.size(); i++)
      result.setQuick(i, getQuick(i) - x.getQuick(i));
    return result;
  }

  @Override
  public Vector normalize() {
    double divSq = Math.sqrt(dot(this));
    return divide(divSq);
  }

  @Override
  public Vector normalize(double power) {
    if (power < 0.0) {
      throw new IllegalArgumentException("Power must be >= 0");
    }
    // we can special case certain powers
    if (Double.isInfinite(power)) {
      return divide(maxValue());
    } else if (power == 2.0) {
      return normalize();
    } else if (power == 1.0) {
      return divide(zSum());
    } else if (power == 0.0) {
      // this is the number of non-zero elements
      double val = 0.0;
      for (int i = 0; i < size(); i++) {
        val += getQuick(i) == 0 ? 0 : 1;
      }
      return divide(val);
    } else {
      double val = 0.0;
      for (int i = 0; i < size(); i++) {
        val += Math.pow(getQuick(i), power);
      }
      double divFactor = Math.pow(val, 1.0 / power);
      return divide(divFactor);
    }
  }

  @Override
  public double maxValue() {
    double result = Double.MIN_VALUE;
    for (int i = 0; i < size(); i++) {
      result = Math.max(result, getQuick(i));
    }
    return result;
  }

  @Override
  public int maxValueIndex() {
    int result = -1;
    double max = Double.MIN_VALUE;
    for (int i = 0; i < size(); i++) {
      double tmp = getQuick(i);
      if (tmp > max) {
        max = tmp;
        result = i;
      }
    }
    return result;
  }

  @Override
  public Vector plus(double x) {
    Vector result = clone();
    for (int i = 0; i < result.size(); i++)
      result.setQuick(i, getQuick(i) + x);
    return result;
  }

  @Override
  public Vector plus(Vector x) {
    if (size() != x.size())
      throw new CardinalityException();
    Vector result = clone();
    for (int i = 0; i < result.size(); i++)
      result.setQuick(i, getQuick(i) + x.getQuick(i));
    return result;
  }

  @Override
  public void set(int index, double value) {
    if (index >= 0 && index < size())
      setQuick(index, value);
    else
      throw new IndexException();
  }

  @Override
  public Vector times(double x) {
    Vector result = clone();
    for (int i = 0; i < result.size(); i++)
      result.setQuick(i, getQuick(i) * x);
    return result;
  }

  @Override
  public Vector times(Vector x) {
    if (size() != x.size())
      throw new CardinalityException();
    Vector result = clone();
    for (int i = 0; i < result.size(); i++)
      result.setQuick(i, getQuick(i) * x.getQuick(i));
    return result;
  }

  @Override
  public double zSum() {
    double result = 0;
    for (int i = 0; i < size(); i++)
      result += getQuick(i);
    return result;
  }

  @Override
  public Vector assign(double value) {
    for (int i = 0; i < size(); i++)
      setQuick(i, value);
    return this;
  }

  @Override
  public Vector assign(double[] values) {
    if (values.length != size())
      throw new CardinalityException();
    for (int i = 0; i < size(); i++)
      setQuick(i, values[i]);
    return this;
  }

  @Override
  public Vector assign(Vector other) {
    if (other.size() != size())
      throw new CardinalityException();
    for (int i = 0; i < size(); i++)
      setQuick(i, other.getQuick(i));
    return this;
  }

  @Override
  public Vector assign(BinaryFunction f, double y) {
    for (int i = 0; i < size(); i++) {
      setQuick(i, f.apply(getQuick(i), y));
    }
    return this;
  }

  @Override
  public Vector assign(UnaryFunction function) {
    for (int i = 0; i < size(); i++)
      setQuick(i, function.apply(getQuick(i)));
    return this;
  }

  @Override
  public Vector assign(Vector other, BinaryFunction function) {
    if (other.size() != size())
      throw new CardinalityException();
    for (int i = 0; i < size(); i++)
      setQuick(i, function.apply(getQuick(i), other.getQuick(i)));
    return this;
  }

  @Override
  public Matrix cross(Vector other) {
    Matrix result = matrixLike(size(), other.size());
    for (int row = 0; row < size(); row++)
      result.assignRow(row, other.times(getQuick(row)));
    return result;
  }

  /**
   * Decodes a point from its WritableComparable<?> representation.
   * 
   * @param writableComparable a WritableComparable<?> produced by
   *        asWritableComparable. Note the payload remainder: it is optional,
   *        but can be present.
   * @return the n-dimensional point
   */
  public static Vector decodeVector(WritableComparable<?> writableComparable) {
    return decodeVector(writableComparable.toString());
  }

  /**
   * Decodes a point from its string representation.
   * 
   * @param formattedString a formatted String produced by asFormatString. Note
   *        the payload remainder: it is optional, but can be present.
   * @return the n-dimensional point
   */
  public static Vector decodeVector(String formattedString) {
    Type vectorType = new TypeToken<Vector>() {
    }.getType();
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(vectorType, new JsonVectorAdapter());
    Gson gson = builder.create();
    return gson.fromJson(formattedString, vectorType);
  }

  public String getName() {
    return name;

  }

  public void setName(String name) {
    this.name = name;

  }/* (non-Javadoc)
   * @see org.apache.mahout.matrix.Vector#asFormatString()
   */
  public String asFormatString(){
    Type vectorType = new TypeToken<Vector>() {
    }.getType();
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(vectorType, new JsonVectorAdapter());
    Gson gson = builder.create();
    return gson.toJson(this, vectorType);
  }

  /**
   * Compare whether two Vector implementations have the same elements, regardless of the
   * implementation and name. Two Vectors are equivalent if they have the same cardinality
   * and all of their values are the same.
   * <p/>
   * Does not compare {@link Vector#getName()}.
   * 
   * 
   * @param left The left hand Vector to compare
   * @param right The right hand Vector
   * @return true if the two Vectors have the same cardinality and the same
   *         values
   *
   * @see #strictEquivalence(Vector, Vector)
   * @see Vector#equals(Object) 
   */
  public static boolean equivalent(Vector left, Vector right) {
    if (left == right) return true;
    boolean result = true;
    int leftCardinality = left.size();
    if (leftCardinality == right.size()) {
      for (int i = 0; i < leftCardinality; i++) {
        if (left.getQuick(i) != right.getQuick(i)) {
          return false;
        }

      }
    } else {
      return false;
    }
    return result;
  }

  /**
   * Compare whether two Vector implementations are the same, including the
   * underlying implementation. Two Vectors are the same if they have the same cardinality, same name
   * and all of their values are the same.
   *
   *
   * @param left The left hand Vector to compare
   * @param right The right hand Vector
   * @return true if the two Vectors have the same cardinality and the same
   *         values
   */
  public static boolean strictEquivalence(Vector left, Vector right) {
    if (left == right) return true;
    if (!(left.getClass().equals(right.getClass()))) return false;
    String leftName = left.getName();
    String rightName = right.getName();
    if (leftName != null && rightName != null && !leftName.equals(rightName)){
      return false;
    } else if ((leftName != null && rightName == null) || (rightName != null && leftName == null)){
      return false;
    }
    
    boolean result = true;
    int leftCardinality = left.size();
    if (leftCardinality == right.size()) {
      for (int i = 0; i < leftCardinality; i++) {
        if (left.getQuick(i) != right.getQuick(i)) {
          return false;
        }

      }
    } else {
      return false;
    }
    return result;
  }


  /* (non-Javadoc)
   * @see org.apache.mahout.matrix.Vector#get(java.lang.String)
   */
  @Override
  public double get(String label) throws IndexException, UnboundLabelException {
    if (bindings == null)
      throw new UnboundLabelException();
    Integer index = bindings.get(label);
    if (index == null)
      throw new UnboundLabelException();
    return get(index);
  }

  /* (non-Javadoc)
   * @see org.apache.mahout.matrix.Vector#getLabelBindings()
   */
  @Override
  public Map<String, Integer> getLabelBindings() {
    return bindings;
  }

  /* (non-Javadoc)
   * @see org.apache.mahout.matrix.Vector#set(java.lang.String, double)
   */
  @Override
  public void set(String label, double value) throws IndexException,
      UnboundLabelException {
    if (bindings == null)
      throw new UnboundLabelException();
    Integer index = bindings.get(label);
    if (index == null)
      throw new UnboundLabelException();
    set(index, value);
  }

  /* (non-Javadoc)
   * @see org.apache.mahout.matrix.Vector#setLabelBindings(java.util.Map)
   */
  @Override
  public void setLabelBindings(Map<String, Integer> bindings) {
    this.bindings = bindings;
  }

  /* (non-Javadoc)
   * @see org.apache.mahout.matrix.Vector#set(java.lang.String, int, double)
   */
  @Override
  public void set(String label, int index, double value) throws IndexException {
    if (bindings == null)
      bindings = new HashMap<String, Integer>();
    bindings.put(label, index);
    set(index, value);
  }

}
